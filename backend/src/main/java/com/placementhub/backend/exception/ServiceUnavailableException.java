package com.placementhub.backend.exception;

import org.springframework.http.HttpStatus;

/** A downstream dependency (e.g. the Gemini API) failed; the message is safe to show users. */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message);
    }
}
