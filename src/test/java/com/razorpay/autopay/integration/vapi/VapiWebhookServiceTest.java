package com.razorpay.autopay.integration.vapi;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.FailureReason;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.CustomerRepository;
import com.razorpay.autopay.service.CallService;
import com.razorpay.autopay.integration.vapi.dto.VapiWebhookCall;
import com.razorpay.autopay.integration.vapi.dto.VapiWebhookMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VapiWebhookServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private CustomerRepository customerRepository;

    private final AtomicReference<Call> storedCall = new AtomicReference<>();
    private VapiWebhookService webhookService;

    @BeforeEach
    void setUp() {
        storedCall.set(null);
        CallService callService = new CallService(callRepository, customerRepository);
        webhookService = new VapiWebhookService(callService);
        when(callRepository.findByVapiCallId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(storedCall.get()));
        when(callRepository.save(any(Call.class))).thenAnswer(invocation -> {
            Call call = invocation.getArgument(0);
            storedCall.set(call);
            return call;
        });
    }

    @Test
    void createsDashboardCallForDemoCustomerFromStatusUpdate() {
        OffsetDateTime vapiStartedAt = OffsetDateTime.parse("2026-10-02T09:00:00-04:00");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(demoCustomer()));

        assertTrue(webhookService.handle(event("status-update", "in-progress",
                "dashboard-call-1", null, vapiStartedAt, null, null)));

        Call call = storedCall.get();
        assertNotNull(call);
        assertEquals(1L, call.getCustomer().getId());
        assertEquals("dashboard-call-1", call.getVapiCallId());
        assertEquals(CallStatus.IN_PROGRESS, call.getStatus());
        assertEquals(LocalDateTime.parse("2026-10-02T13:00:00"), call.getStartedAt());
        verify(customerRepository).findById(1L);
    }

    @Test
    void updatesExistingOutboundCallByVapiId() {
        Call outboundCall = Call.builder()
                .id(17L)
                .customer(demoCustomer())
                .vapiCallId("outbound-vapi-id")
                .startedAt(LocalDateTime.parse("2026-10-02T10:00:00"))
                .status(CallStatus.INITIATED)
                .build();
        storedCall.set(outboundCall);
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-10-02T10:00:00Z");

        assertTrue(webhookService.handle(event("status-update", "in-progress",
                "outbound-vapi-id", null, null, startedAt, null)));

        assertSame(outboundCall, storedCall.get());
        assertEquals(CallStatus.IN_PROGRESS, outboundCall.getStatus());
        assertEquals(17L, outboundCall.getId());
        verify(customerRepository, never()).findById(1L);
    }

    @Test
    void repeatedWebhookEventReusesTheSameCallRow() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(demoCustomer()));
        VapiWebhookMessage startEvent = event("status-update", "in-progress",
                "replayed-vapi-id", null, null,
                OffsetDateTime.parse("2026-10-02T11:00:00Z"), null);

        assertTrue(webhookService.handle(startEvent));
        Call firstSavedCall = storedCall.get();
        assertTrue(webhookService.handle(startEvent));

        assertSame(firstSavedCall, storedCall.get());
        assertEquals("replayed-vapi-id", storedCall.get().getVapiCallId());
        verify(customerRepository, times(1)).findById(1L);
        verify(callRepository, times(2)).save(firstSavedCall);
    }

    @Test
    void endedStatusCompletesCallAndCalculatesDuration() {
        Call call = Call.builder()
                .id(23L)
                .customer(demoCustomer())
                .vapiCallId("duration-vapi-id")
                .startedAt(LocalDateTime.parse("2026-10-02T12:00:00"))
                .status(CallStatus.IN_PROGRESS)
                .build();
        storedCall.set(call);

        assertTrue(webhookService.handle(event("status-update", "ended",
                "duration-vapi-id", null, null, null,
                OffsetDateTime.parse("2026-10-02T12:02:45Z"))));

        assertEquals(CallStatus.COMPLETED, call.getStatus());
        assertEquals(LocalDateTime.parse("2026-10-02T12:02:45"), call.getEndedAt());
        assertEquals(165L, call.getDurationSeconds());
    }

    @Test
    void endOfCallReportCreatesCompletedCallAndPreservesExistingOutcome() {
        Call call = Call.builder()
                .id(29L)
                .customer(demoCustomer())
                .vapiCallId("report-vapi-id")
                .startedAt(LocalDateTime.parse("2026-10-02T14:00:00"))
                .status(CallStatus.IN_PROGRESS)
                .outcome(RecoveryOutcome.PAYMENT_LINK_SENT)
                .build();
        storedCall.set(call);

        assertTrue(webhookService.handle(event("end-of-call-report", null,
                "report-vapi-id", "assistant-ended-call", null, null,
                OffsetDateTime.parse("2026-10-02T14:04:00Z"))));

        assertEquals(CallStatus.COMPLETED, call.getStatus());
        assertEquals(RecoveryOutcome.PAYMENT_LINK_SENT, call.getOutcome());
        assertEquals(240L, call.getDurationSeconds());
        assertEquals(LocalDateTime.parse("2026-10-02T14:04:00"), call.getEndedAt());
    }

    private static VapiWebhookMessage event(String type, String status, String callId,
                                             String endedReason, OffsetDateTime callStartedAt,
                                             OffsetDateTime messageStartedAt, OffsetDateTime messageEndedAt) {
        VapiWebhookCall call = new VapiWebhookCall(callId, null, callStartedAt, null, endedReason);
        return new VapiWebhookMessage(type, status, call, messageStartedAt, messageEndedAt, endedReason);
    }

    private static Customer demoCustomer() {
        return Customer.builder()
                .id(1L)
                .name("Aarav Mehta")
                .phone("+91-90000-00001")
                .amountDue(new BigDecimal("1250.00"))
                .paymentStatus(PaymentStatus.FAILED)
                .failureReason(FailureReason.INSUFFICIENT_FUNDS)
                .createdAt(LocalDateTime.parse("2026-10-01T10:00:00"))
                .build();
    }
}
