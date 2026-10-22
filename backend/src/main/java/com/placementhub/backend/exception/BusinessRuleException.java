package com.placementhub.backend.exception;

import org.springframework.http.HttpStatus;

/** The request is well-formed but a business rule forbids it (e.g. job not open). */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
