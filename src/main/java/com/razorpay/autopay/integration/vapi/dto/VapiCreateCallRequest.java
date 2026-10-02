package com.razorpay.autopay.integration.vapi.dto;

public record VapiCreateCallRequest(
        String assistantId,
        String phoneNumberId,
        VapiCustomerRequest customer
) {
}
