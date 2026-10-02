package com.razorpay.autopay.service;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallServiceTest {

    @Test
    void reusesWebhookCallWhenWebhookArrivesBeforeCreateCallResponseIsStored() {
        CallRepository callRepository = mock(CallRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        var customer = com.razorpay.autopay.entity.Customer.builder().id(12L).build();
        Call pending = Call.builder().id(20L).customer(customer).vapiCallId("pending-20")
                .startedAt(LocalDateTime.now()).build();
        Call webhookCall = Call.builder().id(21L).customer(customer).vapiCallId("vapi-123")
                .startedAt(LocalDateTime.now()).build();
        when(callRepository.findById(20L)).thenReturn(Optional.of(pending));
        when(callRepository.findByVapiCallId("vapi-123")).thenReturn(Optional.of(webhookCall));

        Call assigned = new CallService(callRepository, customerRepository)
                .assignVapiCallId(20L, "vapi-123");

        assertSame(webhookCall, assigned);
        verify(callRepository).delete(pending);
        verify(callRepository, never()).save(pending);
    }
}
