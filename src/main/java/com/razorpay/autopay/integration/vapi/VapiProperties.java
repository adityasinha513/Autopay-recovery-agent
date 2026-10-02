package com.razorpay.autopay.integration.vapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vapi")
public record VapiProperties(
        String apiKey,
        String baseUrl,
        String assistantId,
        String phoneNumberId
) {
}
