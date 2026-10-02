package com.razorpay.autopay.controller;

import com.razorpay.autopay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{customerId}")
    public Map<String, Object> getPaymentStatus(@PathVariable Long customerId) {
        return paymentService.getPaymentStatus(customerId);
    }

    @PostMapping("/{customerId}/retry")
    public Map<String, Object> retryPayment(@PathVariable Long customerId) {
        return paymentService.retryPayment(customerId);
    }

    @PostMapping("/{customerId}/link")
    public Map<String, Object> generatePaymentLink(@PathVariable Long customerId) {
        return paymentService.generatePaymentLink(customerId);
    }
}