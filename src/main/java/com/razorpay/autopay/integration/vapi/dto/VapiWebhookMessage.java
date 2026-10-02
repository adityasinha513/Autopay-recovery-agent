package com.razorpay.autopay.integration.vapi.dto;

import java.time.OffsetDateTime;

public record VapiWebhookMessage(
        String type,
        String status,
        VapiWebhookCall call,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String endedReason
) {
}
