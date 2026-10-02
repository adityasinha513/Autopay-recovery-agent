package com.razorpay.autopay.dto;

import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentStatusResponse(
        Long customerId,
        PaymentStatus paymentStatus,
        BigDecimal amountDue,
        FailureReason failureReason,
        String message
) {
}
