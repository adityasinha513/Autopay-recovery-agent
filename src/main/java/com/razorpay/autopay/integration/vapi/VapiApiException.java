package com.razorpay.autopay.integration.vapi;

import org.springframework.http.HttpStatus;

public class VapiApiException extends RuntimeException {

    private final HttpStatus status;

    public VapiApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public VapiApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
