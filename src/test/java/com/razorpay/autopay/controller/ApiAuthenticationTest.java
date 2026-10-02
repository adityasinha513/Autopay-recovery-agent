package com.razorpay.autopay.controller;

import com.razorpay.autopay.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CustomerController.class, HealthController.class})
@TestPropertySource(properties = "API_KEY=test-api-key")
class ApiAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void customerApiRejectsMissingAndInvalidKeysWithJson401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
        mockMvc.perform(get("/api/customers").header("X-API-Key", "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\":\"UNAUTHORIZED\"}"));
    }

    @Test
    void customerApiAcceptsValidKey() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of());
        mockMvc.perform(get("/api/customers").header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
