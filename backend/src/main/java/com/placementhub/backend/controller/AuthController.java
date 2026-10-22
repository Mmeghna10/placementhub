package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth", description = "Register and log in. Public: no token needed. Rate limited per IP (429 RATE_LIMITED).")
@SecurityRequirements
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a STUDENT or RECRUITER account", description = "201 with a JWT. 400 VALIDATION_FAILED (also for role ADMIN), 409 EMAIL_ALREADY_REGISTERED, 429 RATE_LIMITED.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Log in and get a JWT", description = "401 INVALID_CREDENTIALS or ACCOUNT_DISABLED, 429 RATE_LIMITED (10 attempts per minute per IP).")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}