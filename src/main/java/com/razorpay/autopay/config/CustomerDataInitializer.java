package com.razorpay.autopay.config;

import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomerDataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    createCustomer("Aarav Mehta", "+91-90000-00001", "1250.00", FailureReason.INSUFFICIENT_FUNDS),
                    createCustomer("Diya Nair", "+91-90000-00002", "2380.50", FailureReason.CARD_EXPIRED),
                    createCustomer("Kabir Shah", "+91-90000-00003", "975.00", FailureReason.BANK_DECLINED),
                    createCustomer("Ananya Iyer", "+91-90000-00004", "1840.00", FailureReason.UNKNOWN),
                    createCustomer("Ishaan Rao", "+91-90000-00005", "3200.00", FailureReason.AUTHENTICATION_FAILED),
                    createCustomer("Meera Kulkarni", "+91-90000-00006", "1499.00", FailureReason.UNKNOWN),
                    createCustomer("Rohan Desai", "+91-90000-00007", "760.00", FailureReason.UNKNOWN),
                    createCustomer("Saanvi Kapoor", "+91-90000-00008", "2100.00", FailureReason.UNKNOWN),
                    createCustomer("Arjun Menon", "+91-90000-00009", "1325.00", FailureReason.UNKNOWN),
                    createCustomer("Tara Banerjee", "+91-90000-00010", "2890.00", FailureReason.TECHNICAL_ERROR)
            ));
        }
    }

    private Customer createCustomer(String name, String phone, String amountDue, FailureReason failureReason) {
        return Customer.builder()
                .name(name)
                .phone(phone)
                .amountDue(new BigDecimal(amountDue))
                .paymentStatus(PaymentStatus.FAILED)
                .failureReason(failureReason)
                .build();
    }
}