package com.placementhub.backend.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but does not own the resource. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
