package com.razorpay.autopay.dto;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;

import java.time.LocalDateTime;

public record CallSummaryResponse(
        Long callId,
        Long customerId,
        String customerName,
        CallStatus status,
        RecoveryOutcome outcome,
        Long durationSeconds,
        LocalDateTime startedAt
) {
    public static CallSummaryResponse from(Call call) {
        return new CallSummaryResponse(
                call.getId(),
                call.getCustomer().getId(),
                call.getCustomer().getName(),
                call.getStatus(),
                call.getOutcome(),
                call.getDurationSeconds(),
                call.getStartedAt()
        );
    }
}
