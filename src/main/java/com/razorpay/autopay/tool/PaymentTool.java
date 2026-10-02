package com.razorpay.autopay.tool;

import com.razorpay.autopay.dto.PaymentLinkResponse;
import com.razorpay.autopay.dto.PaymentStatusResponse;
import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.service.PaymentService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentTool {

    private final PaymentService paymentService;

    public PaymentTool(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public PaymentStatusResponse checkPaymentStatus(Long customerId) {
        return toPaymentStatusResponse(paymentService.getPaymentStatus(customerId));
    }

    public PaymentStatusResponse retryPayment(Long customerId) {
        return toPaymentStatusResponse(paymentService.retryPayment(customerId));
    }

    public PaymentLinkResponse sendPaymentLink(Long customerId) {
        Map<String, Object> result = paymentService.generatePaymentLink(customerId);
        return new PaymentLinkResponse(
                (Long) result.get("customerId"),
                (BigDecimal) result.get("amountDue"),
                (String) result.get("paymentUrl"),
                (String) result.get("message")
        );
    }

    private PaymentStatusResponse toPaymentStatusResponse(Map<String, Object> result) {
        return new PaymentStatusResponse(
                (Long) result.get("customerId"),
                (PaymentStatus) result.get("paymentStatus"),
                (BigDecimal) result.get("amountDue"),
                (FailureReason) result.get("failureReason"),
                (String) result.get("message")
        );
    }
}
