package com.razorpay.autopay.integration.vapi.dto;

import java.util.Map;

public record VapiCreateCallRequest(
        String assistantId,
        String phoneNumberId,
        VapiCustomerRequest customer,
        Map<String, Object> metadata,
        VapiAssistantOverrides assistantOverrides
) {
}
