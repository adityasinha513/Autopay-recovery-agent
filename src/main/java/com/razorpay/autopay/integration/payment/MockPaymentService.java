package com.razorpay.autopay.integration.payment;

import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockPaymentService {

    public PaymentStatus simulatePaymentAttempt(Customer customer) {
        return switch (ThreadLocalRandom.current().nextInt(3)) {
            case 0 -> PaymentStatus.SUCCESS;
            case 1 -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}