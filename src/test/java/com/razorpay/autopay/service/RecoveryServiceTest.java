package com.razorpay.autopay.service;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.entity.RecoveryAction;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.CustomerRepository;
import com.razorpay.autopay.repository.RecoveryActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    private static final List<CallStatus> OUTCOME_ELIGIBLE_STATUSES = List.of(
            CallStatus.INITIATED,
            CallStatus.RINGING,
            CallStatus.IN_PROGRESS,
            CallStatus.COMPLETED);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RecoveryActionRepository recoveryActionRepository;

    @Mock
    private CallRepository callRepository;

    private RecoveryService recoveryService;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Aarav Mehta")
                .phone("+91-90000-00001")
                .amountDue(new BigDecimal("1250.00"))
                .paymentStatus(PaymentStatus.FAILED)
                .failureReason(FailureReason.INSUFFICIENT_FUNDS)
                .createdAt(LocalDateTime.parse("2026-10-01T10:00:00"))
                .build();
        recoveryService = new RecoveryService(customerRepository, recoveryActionRepository, callRepository);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(recoveryActionRepository.save(any(RecoveryAction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recoveryOutcomeUpdatesLatestEligibleCall() {
        Call call = activeCall();
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES)).thenReturn(Optional.of(call));
        when(callRepository.save(call)).thenReturn(call);

        recoveryService.recordOutcome(1L, RecoveryOutcome.RECOVERED);

        assertEquals(RecoveryOutcome.RECOVERED, call.getOutcome());
        verify(callRepository).findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES);
        verify(callRepository).save(call);
    }

    @Test
    void recoveryActionIsStillCreatedWithTheExistingShape() {
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES)).thenReturn(Optional.empty());

        RecoveryAction action = recoveryService.recordOutcome(1L, RecoveryOutcome.PAYMENT_LINK_SENT);

        assertSame(customer, action.getCustomer());
        assertEquals("OUTCOME", action.getActionType());
        assertEquals("PAYMENT_LINK_SENT", action.getStatus());
        verify(recoveryActionRepository).save(action);
    }

    @Test
    void updatingOutcomePreservesCallLifecycleFields() {
        Call call = activeCall();
        call.setStatus(CallStatus.COMPLETED);
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES)).thenReturn(Optional.of(call));
        when(callRepository.save(call)).thenReturn(call);

        recoveryService.recordOutcome(1L, RecoveryOutcome.ESCALATED);

        assertEquals("vapi-call-123", call.getVapiCallId());
        assertEquals(LocalDateTime.parse("2026-10-02T12:00:00"), call.getStartedAt());
        assertEquals(LocalDateTime.parse("2026-10-02T12:03:10"), call.getEndedAt());
        assertEquals(190L, call.getDurationSeconds());
        assertEquals(CallStatus.COMPLETED, call.getStatus());
        assertEquals(RecoveryOutcome.ESCALATED, call.getOutcome());
    }

    @Test
    void noCallStillRecordsRecoveryActionSuccessfully() {
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES)).thenReturn(Optional.empty());

        RecoveryAction action = recoveryService.recordOutcome(1L, RecoveryOutcome.ALREADY_PAID);

        assertEquals("ALREADY_PAID", action.getStatus());
        verify(recoveryActionRepository).save(action);
        verify(callRepository, never()).save(any(Call.class));
    }

    @Test
    void repeatedOutcomeCallsUpdateTheSameCallWithoutCreatingAnother() {
        Call call = activeCall();
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                eq(1L), anyCollection())).thenReturn(Optional.of(call));
        when(callRepository.save(call)).thenReturn(call);

        recoveryService.recordOutcome(1L, RecoveryOutcome.RETRY_SCHEDULED);
        recoveryService.recordOutcome(1L, RecoveryOutcome.RETRY_SCHEDULED);

        assertEquals(91L, call.getId());
        assertEquals(RecoveryOutcome.RETRY_SCHEDULED, call.getOutcome());
        verify(callRepository, times(2)).findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                1L, OUTCOME_ELIGIBLE_STATUSES);
        verify(callRepository, times(2)).save(call);
    }

    @Test
    void allSupportedOutcomesAreCopiedToTheCall() {
        Call call = activeCall();
        when(callRepository.findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
                eq(1L), anyCollection())).thenReturn(Optional.of(call));
        when(callRepository.save(call)).thenReturn(call);

        for (RecoveryOutcome outcome : RecoveryOutcome.values()) {
            recoveryService.recordOutcome(1L, outcome);
            assertEquals(outcome, call.getOutcome());
        }

        assertEquals(7, RecoveryOutcome.values().length);
        verify(recoveryActionRepository, times(7)).save(any(RecoveryAction.class));
        verify(callRepository, times(7)).save(call);
    }

    private Call activeCall() {
        return Call.builder()
                .id(91L)
                .customer(customer)
                .vapiCallId("vapi-call-123")
                .startedAt(LocalDateTime.parse("2026-10-02T12:00:00"))
                .endedAt(LocalDateTime.parse("2026-10-02T12:03:10"))
                .durationSeconds(190L)
                .status(CallStatus.IN_PROGRESS)
                .build();
    }
}
