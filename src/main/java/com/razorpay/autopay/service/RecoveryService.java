package com.razorpay.autopay.service;

import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.entity.RecoveryAction;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.exception.CustomerNotFoundException;
import com.razorpay.autopay.repository.CustomerRepository;
import com.razorpay.autopay.repository.RecoveryActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final CustomerRepository customerRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final CallRepository callRepository;

    public RecoveryAction scheduleCallback(Long customerId) {
        return scheduleCallback(customerId, null);
    }

    public RecoveryAction scheduleCallback(Long customerId, LocalDateTime callbackTime) {
        return recordAction(customerId, "CALLBACK", "SCHEDULED", callbackTime, null);
    }

    public RecoveryAction escalateToSupport(Long customerId) {
        return escalateToSupport(customerId, null);
    }

    public RecoveryAction escalateToSupport(Long customerId, String reason) {
        return recordAction(customerId, "SUPPORT_ESCALATION", "OPEN", null, reason);
    }

    @Transactional
    public RecoveryAction recordOutcome(Long customerId, RecoveryOutcome outcome) {
        RecoveryAction action = recordAction(customerId, "OUTCOME", outcome.name(), null, null);
        callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(customerId, List.of(
                        CallStatus.INITIATED,
                        CallStatus.RINGING,
                        CallStatus.IN_PROGRESS,
                        CallStatus.COMPLETED))
                .ifPresent(call -> {
                    call.setOutcome(outcome);
                    callRepository.save(call);
                });
        return action;
    }

    private RecoveryAction recordAction(Long customerId, String actionType, String status,
                                        LocalDateTime callbackTime, String details) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + customerId));

        RecoveryAction action = RecoveryAction.builder()
                .customer(customer)
                .actionType(actionType)
                .status(status)
                .callbackTime(callbackTime)
                .details(details)
                .build();
        return recoveryActionRepository.save(action);
    }
}
