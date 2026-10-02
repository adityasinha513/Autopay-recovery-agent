package com.razorpay.autopay.service;

import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.exception.CustomerNotFoundException;
import com.razorpay.autopay.integration.payment.MockPaymentService;
import com.razorpay.autopay.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CustomerRepository customerRepository;
    private final MockPaymentService mockPaymentService;

    public Map<String, Object> getPaymentStatus(Long customerId) {
        Customer customer = findCustomer(customerId);
        return statusResponse(customer, "Current payment status retrieved.");
    }

    public Map<String, Object> retryPayment(Long customerId) {
        Customer customer = findCustomer(customerId);
        PaymentStatus paymentStatus = mockPaymentService.simulatePaymentAttempt(customer);

        customer.setPaymentStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.SUCCESS) {
            customer.setFailureReason(null);
        }

        Customer savedCustomer = customerRepository.save(customer);
        return statusResponse(savedCustomer, messageFor(paymentStatus));
    }

    public Map<String, Object> generatePaymentLink(Long customerId) {
        Customer customer = findCustomer(customerId);
        String paymentUrl = "https://pay.autopay-recovery.example/checkout/"
                + customer.getId() + "?token=" + UUID.randomUUID();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("customerId", customer.getId());
        response.put("amountDue", customer.getAmountDue());
        response.put("paymentUrl", paymentUrl);
        response.put("message", "Fictional payment link generated.");
        return response;
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + customerId));
    }

    private Map<String, Object> statusResponse(Customer customer, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("customerId", customer.getId());
        response.put("paymentStatus", customer.getPaymentStatus());
        response.put("amountDue", customer.getAmountDue());
        if (customer.getFailureReason() != null) {
            response.put("failureReason", customer.getFailureReason());
        }
        response.put("message", message);
        return response;
    }

    private String messageFor(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case SUCCESS -> "Payment retry succeeded.";
            case FAILED -> "Payment retry failed.";
            case PENDING -> "Payment retry is pending confirmation.";
        };
    }
}