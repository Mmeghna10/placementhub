package com.placementhub.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placementhub.backend.observability.RequestIdFilter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

/**
 * Single error shape for every failure the API returns.
 * {@code error} stays the first field and holds a human-readable message, because the
 * existing frontend reads {@code response.data.error} (or the first value of the body).
 * {@code requestId} matches the X-Request-Id header and the server's log lines for the
 * request, so an error a user reports can be found in the logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int status,
        String code,
        String path,
        String timestamp,
        String requestId,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return withFieldErrors(status, code, message, path, null);
    }

    public static ErrorResponse withFieldErrors(int status, String code, String message, String path,
                                                Map<String, String> fieldErrors) {
        return new ErrorResponse(message, status, code, path, Instant.now().toString(),
                MDC.get(RequestIdFilter.MDC_KEY), fieldErrors);
    }
}
