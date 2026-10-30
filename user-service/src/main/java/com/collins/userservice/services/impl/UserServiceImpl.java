package com.collins.userservice.services.impl;

import com.collins.userservice.entities.User;
import com.collins.userservice.payloads.requests.LoginRequest;
import com.collins.userservice.payloads.requests.RegisterRequest;
import com.collins.userservice.payloads.responses.AuthResponse;
import com.collins.userservice.payloads.responses.UserResponse;
import com.collins.userservice.repositories.UserRepository;
import com.collins.userservice.services.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;


    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Register user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists" + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .walletBalance(request.getInitialDeposit() != null
                        ?  request.getInitialDeposit()
                        : BigDecimal.valueOf(10000))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered user: {}", savedUser.getId());

        // Publish User Register Event
        Map<String, Object> userRegisteredEvent = new HashMap<>();
        userRegisteredEvent.put("id", savedUser.getId());
        userRegisteredEvent.put("email", savedUser.getEmail());
        userRegisteredEvent.put("firstName", savedUser.getFirstName());
        userRegisteredEvent.put("lastName", savedUser.getLastName());
        userRegisteredEvent.put("walletBalance", savedUser.getWalletBalance());

        kafkaTemplate.send(USER_REGISTERED_TOPIC, savedUser.getId(), userRegisteredEvent);

        String accessToken = generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = generateRefreshToken(savedUser.getId());

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found" + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        log.info("Login successful: {}",user.getId());

        String accessToken = generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = generateRefreshToken(user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private String generateRefreshToken(String userId) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[]  keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    private String generateAccessToken(String userId, String email) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }


    /**
     * Get User By Id
     * Called by other services via Feign(internal)
     * @param userId
     * @return
     */
    @Override
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found" + userId));
        
        return mapToResponse(user);
    }

    /**
     * Add funds to wallet
     * @param userId
     * @param amount
     * @return
     */
    @Override
    public UserResponse addFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found" + userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);

        log.info("Funds added: {} to user:",amount, userId);
        return mapToResponse(savedUser);
    }

    /**
     * Deduct funds from wallet
     * @param userId
     * @param amount
     * @return
     */
    @Override
    public UserResponse deductFunds(String userId, BigDecimal amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + userId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));

        User savedUser = userRepository.save(user);

        log.info("Funds deducted: {} from user: {}", amount, userId);

        return mapToResponse(savedUser);
    }

    /**
     * Credit funds to wallet
     * @param userId
     * @param amount
     * @return
     */
    @Override
    public UserResponse creditFunds(String userId, BigDecimal amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + userId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        user.setWalletBalance(user.getWalletBalance().add(amount));

        User savedUser = userRepository.save(user);

        log.info("Funds credited: {} to user: {}", amount, userId);

        return mapToResponse(savedUser);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .walletBalance(user.getWalletBalance())
                .accessToken(accessToken).refreshToken(refreshToken)
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .walletBalance(user.getWalletBalance())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
