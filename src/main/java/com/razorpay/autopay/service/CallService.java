package com.razorpay.autopay.service;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.exception.CustomerNotFoundException;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final CustomerRepository customerRepository;

    public Call createCall(Long customerId, String vapiCallId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + customerId));

        Call call = Call.builder()
                .customer(customer)
                .vapiCallId(vapiCallId)
                .startedAt(LocalDateTime.now())
                .status(CallStatus.INITIATED)
                .build();
        return callRepository.save(call);
    }

    public Call createCall(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + customerId));

        return callRepository.save(Call.builder()
                .customer(customer)
                .vapiCallId("pending-" + UUID.randomUUID())
                .status(CallStatus.INITIATED)
                .build());
    }

    @Transactional
    public Call assignVapiCallId(Long callId, String vapiCallId) {
        Call call = findCall(callId);
        Optional<Call> webhookCall = callRepository.findByVapiCallId(vapiCallId);
        if (webhookCall.isPresent() && !webhookCall.get().getId().equals(callId)) {
            Call existing = webhookCall.get();
            if (!existing.getCustomer().getId().equals(call.getCustomer().getId())) {
                throw new IllegalStateException("Vapi call is already associated with another customer");
            }
            callRepository.delete(call);
            return existing;
        }
        call.setVapiCallId(vapiCallId);
        return callRepository.save(call);
    }

    @Transactional
    public synchronized Optional<Call> upsertFromVapi(String vapiCallId, CallStatus status,
                                                       LocalDateTime startedAt, LocalDateTime endedAt,
                                                       Long customerId) {
        if (vapiCallId == null || vapiCallId.isBlank() || status == null) {
            return Optional.empty();
        }

        Optional<Call> optionalCall = callRepository.findByVapiCallId(vapiCallId);
        Call call;
        if (optionalCall.isPresent()) {
            call = optionalCall.get();
            if (!isTerminal(call.getStatus())) {
                call.setStatus(status);
            }
        } else {
            if (customerId == null || customerId <= 0) {
                return Optional.empty();
            }
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(
                            "Customer not found with id: " + customerId));
            call = Call.builder()
                    .customer(customer)
                    .vapiCallId(vapiCallId)
                    .startedAt(startedAt != null ? startedAt : LocalDateTime.now())
                    .status(status)
                    .build();
        }

        if (startedAt != null) {
            call.setStartedAt(startedAt);
        }
        if (endedAt != null) {
            call.setEndedAt(endedAt);
            call.setDurationSeconds(Duration.between(call.getStartedAt(), endedAt).getSeconds());
        } else if (isTerminal(status)) {
            finishCall(call);
        }
        return Optional.of(callRepository.save(call));
    }

    public Call updateCall(Long callId, CallStatus status, RecoveryOutcome outcome) {
        Call call = findCall(callId);
        call.setStatus(status);
        call.setOutcome(outcome);
        if (isTerminal(status)) {
            finishCall(call);
        }
        return callRepository.save(call);
    }

    public Call completeCall(Long callId, RecoveryOutcome outcome) {
        Call call = findCall(callId);
        call.setStatus(CallStatus.COMPLETED);
        call.setOutcome(outcome);
        finishCall(call);
        return callRepository.save(call);
    }

    private Call findCall(Long callId) {
        return callRepository.findById(callId)
                .orElseThrow(() -> new EntityNotFoundException("Call not found with id: " + callId));
    }

    private boolean isTerminal(CallStatus status) {
        return status == CallStatus.COMPLETED
                || status == CallStatus.FAILED
                || status == CallStatus.NO_ANSWER;
    }

    private void finishCall(Call call) {
        if (call.getEndedAt() == null) {
            LocalDateTime endedAt = LocalDateTime.now();
            call.setEndedAt(endedAt);
            call.setDurationSeconds(Duration.between(call.getStartedAt(), endedAt).getSeconds());
        }
    }
}
