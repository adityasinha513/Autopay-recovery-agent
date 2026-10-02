package com.razorpay.autopay.dto;

import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerDetailsResponse(
        Long customerId,
        String name,
        String phone,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        FailureReason failureReason,
        LocalDateTime createdAt
) {
}
