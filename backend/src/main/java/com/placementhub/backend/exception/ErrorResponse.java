package com.placementhub.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Single error shape for every failure the API returns.
 * {@code error} stays the first field and holds a human-readable message, because the
 * existing frontend reads {@code response.data.error} (or the first value of the body).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int status,
        String code,
        String path,
        String timestamp,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(message, status, code, path, Instant.now().toString(), null);
    }

    public static ErrorResponse withFieldErrors(int status, String code, String message, String path,
                                                Map<String, String> fieldErrors) {
        return new ErrorResponse(message, status, code, path, Instant.now().toString(), fieldErrors);
    }
}
