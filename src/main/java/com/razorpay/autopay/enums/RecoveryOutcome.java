package com.razorpay.autopay.enums;

public enum RecoveryOutcome {
    RECOVERED,
    PAYMENT_LINK_SENT,
    RETRY_SCHEDULED,
    ESCALATED,
    CUSTOMER_REFUSED,
    ALREADY_PAID,
    FAILED
}