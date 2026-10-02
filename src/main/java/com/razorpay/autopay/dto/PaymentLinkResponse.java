package com.razorpay.autopay.dto;

import java.math.BigDecimal;

public record PaymentLinkResponse(
        Long customerId,
        BigDecimal amountDue,
        String paymentUrl,
        String message
) {
}
