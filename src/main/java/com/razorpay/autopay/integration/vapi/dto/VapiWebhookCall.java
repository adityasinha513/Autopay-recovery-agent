package com.razorpay.autopay.integration.vapi.dto;

import java.time.OffsetDateTime;

public record VapiWebhookCall(
        String id,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String endedReason
) {
}
