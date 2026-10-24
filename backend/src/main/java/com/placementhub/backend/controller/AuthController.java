package com.placementhub.backend.controller;

import com.placementhub.backend.config.ApiExamples;
import com.placementhub.backend.dto.*;
import com.placementhub.backend.exception.ErrorResponse;
import com.placementhub.backend.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Register a STUDENT or RECRUITER account", description = "201 with a JWT. 400 VALIDATION_FAILED (also for role ADMIN), 409 EMAIL_ALREADY_REGISTERED, 429 RATE_LIMITED.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Student", value = ApiExamples.REGISTER_REQUEST))))
    @ApiResponse(responseCode = "201", description = "Account created", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = AuthResponse.class), examples = @ExampleObject(value = ApiExamples.AUTH_RESPONSE)))
    @ApiResponse(responseCode = "400", description = "Invalid fields", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = ApiExamples.VALIDATION_FAILED)))
    @ApiResponse(responseCode = "409", description = "Email taken", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = ApiExamples.EMAIL_ALREADY_REGISTERED)))
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Log in and get a JWT", description = "401 INVALID_CREDENTIALS or ACCOUNT_DISABLED, 429 RATE_LIMITED (10 attempts per minute per IP).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Login", value = ApiExamples.LOGIN_REQUEST))))
    @ApiResponse(responseCode = "200", description = "Logged in", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = AuthResponse.class), examples = @ExampleObject(value = ApiExamples.AUTH_RESPONSE)))
    @ApiResponse(responseCode = "401", description = "Wrong email or password", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = ApiExamples.INVALID_CREDENTIALS)))
    @ApiResponse(responseCode = "429", description = "Too many attempts", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = ApiExamples.RATE_LIMITED)))
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
