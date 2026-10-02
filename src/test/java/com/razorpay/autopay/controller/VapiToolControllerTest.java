package com.razorpay.autopay.controller;

import com.razorpay.autopay.dto.CustomerDetailsResponse;
import com.razorpay.autopay.dto.PaymentStatusResponse;
import com.razorpay.autopay.enums.PaymentStatus;
import com.razorpay.autopay.exception.CustomerNotFoundException;
import com.razorpay.autopay.tool.CustomerTool;
import com.razorpay.autopay.tool.PaymentTool;
import com.razorpay.autopay.tool.RecoveryTool;
import com.razorpay.autopay.tool.SupportTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VapiToolController.class)
@TestPropertySource(properties = "API_KEY=test-api-key")
class VapiToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerTool customerTool;

    @MockitoBean
    private PaymentTool paymentTool;

    @MockitoBean
    private RecoveryTool recoveryTool;

    @MockitoBean
    private SupportTool supportTool;

    @Test
    void getCustomerDetailsDelegatesAndReturnsStructuredResponse() throws Exception {
        when(customerTool.getCustomerDetails(42L)).thenReturn(new CustomerDetailsResponse(
                42L, "Demo Customer", "+15550000000", new BigDecimal("125.00"),
                PaymentStatus.FAILED, null, LocalDateTime.parse("2026-10-01T10:00:00")));

        mockMvc.perform(post("/api/tools/vapi/get_customer_details").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.name").value("Demo Customer"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        verify(customerTool).getCustomerDetails(42L);
    }

    @Test
    void checkPaymentStatusDelegatesAndReturnsStructuredResponse() throws Exception {
        when(paymentTool.checkPaymentStatus(42L)).thenReturn(new PaymentStatusResponse(
                42L, PaymentStatus.FAILED, new BigDecimal("125.00"), null, "Status checked"));

        mockMvc.perform(post("/api/tools/vapi/check_payment_status").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.amountDue").value(125.00));

        verify(paymentTool).checkPaymentStatus(42L);
    }

    @Test
    void retryPaymentDelegatesAndReturnsStructuredResponse() throws Exception {
        when(paymentTool.retryPayment(42L)).thenReturn(new PaymentStatusResponse(
                42L, PaymentStatus.SUCCESS, BigDecimal.ZERO, null, "Mock retry succeeded"));

        mockMvc.perform(post("/api/tools/vapi/retry_payment").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Mock retry succeeded"));

        verify(paymentTool).retryPayment(42L);
    }

    @Test
    void unknownToolReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tools/vapi/not_a_tool").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_TOOL"));

        verifyNoInteractions(customerTool, paymentTool, recoveryTool, supportTool);
    }

    @Test
    void invalidArgumentsReturnBadRequestWithoutCallingTool() throws Exception {
        mockMvc.perform(post("/api/tools/vapi/check_payment_status").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENTS"));

        verifyNoInteractions(customerTool, paymentTool, recoveryTool, supportTool);
    }

    @Test
    void customerNotFoundReturnsStructuredJsonError() throws Exception {
        doThrow(new CustomerNotFoundException("Customer not found with id: 999"))
                .when(paymentTool).checkPaymentStatus(999L);

        mockMvc.perform(post("/api/tools/vapi/check_payment_status").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }

    @Test
    void unexpectedToolFailureReturnsStructuredJsonError() throws Exception {
        when(paymentTool.checkPaymentStatus(42L)).thenThrow(new IllegalStateException("database detail"));

        mockMvc.perform(post("/api/tools/vapi/check_payment_status").header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":42}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("TOOL_EXECUTION_FAILED"))
                .andExpect(jsonPath("$.message").value("The requested tool could not be completed."));
    }
}
