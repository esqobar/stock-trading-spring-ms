package com.collins.userservice.resources;

import com.collins.userservice.payloads.requests.LoginRequest;
import com.collins.userservice.payloads.requests.RegisterRequest;
import com.collins.userservice.payloads.responses.AuthResponse;
import com.collins.userservice.payloads.responses.UserResponse;
import com.collins.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@RequestHeader("x-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(userId));
    }

    @PostMapping("/{userId}/funds/add")
    public ResponseEntity<UserResponse> addFunds(@PathVariable String userId, @RequestParam BigDecimal amount) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.addFunds(userId, amount));
    }

    @PostMapping("/{userId}/funds/deduct")
    public ResponseEntity<UserResponse> deductFunds(@PathVariable String userId, @RequestParam BigDecimal amount) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.deductFunds(userId, amount));
    }

    @PostMapping("/{userId}/funds/credit")
    public ResponseEntity<UserResponse> creditFunds(@PathVariable String userId, @RequestParam BigDecimal amount) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.creditFunds(userId, amount));
    }
}
