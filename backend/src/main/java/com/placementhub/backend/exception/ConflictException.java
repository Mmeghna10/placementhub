package com.placementhub.backend.exception;

import org.springframework.http.HttpStatus;

/** The request clashes with the current state of the data (duplicates, dependent rows). */
public class ConflictException extends ApiException {
    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
