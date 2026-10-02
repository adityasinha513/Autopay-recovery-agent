package com.razorpay.autopay.dto;

import com.razorpay.autopay.enums.CallStatus;

public record OutboundCallResponse(
        Long customerId,
        Long callId,
        String vapiCallId,
        CallStatus status
) {
}
