package com.razorpay.autopay.dto;

public record DashboardMetricsResponse(
        long totalCalls,
        long completedCalls,
        double recoveryRate,
        long averageCallDurationSeconds,
        long paymentLinksSent,
        long escalations
) {
}
