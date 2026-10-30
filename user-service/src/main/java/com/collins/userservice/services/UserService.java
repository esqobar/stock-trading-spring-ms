package com.collins.userservice.services;

import com.collins.userservice.payloads.requests.LoginRequest;
import com.collins.userservice.payloads.requests.RegisterRequest;
import com.collins.userservice.payloads.responses.AuthResponse;
import com.collins.userservice.payloads.responses.UserResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;

public interface UserService {
    AuthResponse register(@Valid RegisterRequest request);

    AuthResponse login(@Valid LoginRequest request);

    UserResponse getUserById(String userId);

    UserResponse addFunds(String userId, BigDecimal amount);

    UserResponse deductFunds(String userId, BigDecimal amount);

    UserResponse creditFunds(String userId, BigDecimal amount);
}
