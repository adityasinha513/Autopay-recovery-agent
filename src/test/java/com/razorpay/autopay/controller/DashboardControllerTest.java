package com.razorpay.autopay.controller;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.entity.RecoveryAction;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.RecoveryActionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@TestPropertySource(properties = "API_KEY=test-api-key")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CallRepository callRepository;

    @MockitoBean
    private RecoveryActionRepository recoveryActionRepository;

    @Test
    void metricsAreCalculatedFromRecordedCallsAndActions() throws Exception {
        Customer customer = Customer.builder().id(12L).name("Demo Customer").build();
        Call call = Call.builder()
                .id(3L)
                .customer(customer)
                .status(CallStatus.COMPLETED)
                .outcome(RecoveryOutcome.RECOVERED)
                .durationSeconds(78L)
                .startedAt(LocalDateTime.parse("2026-10-02T10:00:00"))
                .build();
        RecoveryAction paymentLink = RecoveryAction.builder()
                .actionType("OUTCOME").status("PAYMENT_LINK_SENT").build();
        RecoveryAction escalation = RecoveryAction.builder()
                .actionType("SUPPORT_ESCALATION").status("OPEN").build();
        when(callRepository.findAll()).thenReturn(List.of(call));
        when(recoveryActionRepository.findAll()).thenReturn(List.of(paymentLink, escalation));

        mockMvc.perform(get("/api/metrics").header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(1))
                .andExpect(jsonPath("$.completedCalls").value(1))
                .andExpect(jsonPath("$.recoveryRate").value(100.0))
                .andExpect(jsonPath("$.averageCallDurationSeconds").value(78))
                .andExpect(jsonPath("$.paymentLinksSent").value(1))
                .andExpect(jsonPath("$.escalations").value(1));
    }
}
