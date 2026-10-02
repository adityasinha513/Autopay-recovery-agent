package com.razorpay.autopay.enums;

public enum FailureReason {
    INSUFFICIENT_FUNDS,
    CARD_EXPIRED,
    BANK_DECLINED,
    AUTHENTICATION_FAILED,
    TECHNICAL_ERROR,
    UNKNOWN
}