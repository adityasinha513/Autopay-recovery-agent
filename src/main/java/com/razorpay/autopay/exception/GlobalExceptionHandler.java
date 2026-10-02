package com.razorpay.autopay.exception;

import com.razorpay.autopay.integration.vapi.VapiApiException;
import com.razorpay.autopay.integration.vapi.dto.VapiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<String> handleCustomerNotFound(CustomerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(VapiApiException.class)
    public ResponseEntity<VapiErrorResponse> handleVapiApiFailure(VapiApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new VapiErrorResponse(exception.getMessage()));
    }
}
