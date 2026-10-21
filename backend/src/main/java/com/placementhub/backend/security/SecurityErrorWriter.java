package com.placementhub.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementhub.backend.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes the same JSON ErrorResponse as GlobalExceptionHandler for failures that happen
 * inside the security filter chain (before any controller runs).
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** No valid credentials: missing, malformed, expired or revoked token. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (request.getAttribute(JwtFilter.AUTH_ERROR_ATTRIBUTE) instanceof JwtFilter.AuthError error) {
            write(request, response, HttpStatus.UNAUTHORIZED, error.name(), error.getMessage());
        } else {
            write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
    }

    /** Authenticated, but the role does not allow this URL. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status.value(), code, message, request.getRequestURI()));
    }
}
