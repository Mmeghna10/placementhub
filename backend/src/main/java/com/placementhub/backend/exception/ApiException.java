package com.placementhub.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. Services throw a subclass and
 * GlobalExceptionHandler turns it into an ErrorResponse with the right HTTP status.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
