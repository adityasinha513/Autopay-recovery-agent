package com.razorpay.autopay.integration.vapi;

import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.integration.vapi.dto.VapiWebhookCall;
import com.razorpay.autopay.integration.vapi.dto.VapiWebhookMessage;
import com.razorpay.autopay.service.CallService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class VapiWebhookService {

    private final CallService callService;

    public VapiWebhookService(CallService callService) {
        this.callService = callService;
    }

    public boolean handle(VapiWebhookMessage message) {
        if (message == null || message.call() == null || !StringUtils.hasText(message.call().id())) {
            return false;
        }

        String type = message.type();
        CallStatus status;
        if ("status-update".equals(type)) {
            status = mapStatus(firstText(message.status(), message.call().status()),
                    firstText(message.endedReason(), message.call().endedReason()));
        } else if ("assistant.started".equals(type)) {
            status = CallStatus.IN_PROGRESS;
        } else if ("end-of-call-report".equals(type)) {
            status = mapStatus("ended", firstText(message.endedReason(), message.call().endedReason()));
        } else {
            return false;
        }
        if (status == null) {
            return false;
        }

        VapiWebhookCall vapiCall = message.call();
        LocalDateTime startedAt = toUtcLocalDateTime(firstTimestamp(message.startedAt(), vapiCall.startedAt()));
        LocalDateTime endedAt = toUtcLocalDateTime(firstTimestamp(message.endedAt(), vapiCall.endedAt()));

        return callService.upsertFromVapi(vapiCall.id(), status, startedAt, endedAt).isPresent();
    }

    private CallStatus mapStatus(String status, String endedReason) {
        String normalized = status == null ? "" : status.toLowerCase().replace('_', '-');
        String reason = endedReason == null ? "" : endedReason.toLowerCase().replace('_', '-');

        return switch (normalized) {
            case "scheduled", "queued", "initiated" -> CallStatus.INITIATED;
            case "ringing" -> CallStatus.RINGING;
            case "in-progress", "started" -> CallStatus.IN_PROGRESS;
            case "failed" -> CallStatus.FAILED;
            case "no-answer", "noanswer" -> CallStatus.NO_ANSWER;
            case "ended", "completed" -> {
                if (reason.contains("no-answer") || reason.contains("did-not-answer")) {
                    yield CallStatus.NO_ANSWER;
                }
                if (reason.contains("fail") || reason.contains("error") || reason.contains("start-error")) {
                    yield CallStatus.FAILED;
                }
                yield CallStatus.COMPLETED;
            }
            default -> null;
        };
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private OffsetDateTime firstTimestamp(OffsetDateTime preferred, OffsetDateTime fallback) {
        return preferred != null ? preferred : fallback;
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime timestamp) {
        return timestamp == null ? null : timestamp.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
