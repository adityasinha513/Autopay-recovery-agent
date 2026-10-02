package com.razorpay.autopay.integration.vapi.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record VapiWebhookCall(
        String id,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String endedReason,
        Map<String, Object> metadata
) {
}
