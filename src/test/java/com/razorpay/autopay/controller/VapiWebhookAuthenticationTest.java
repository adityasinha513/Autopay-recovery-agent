package com.razorpay.autopay.controller;

import com.razorpay.autopay.integration.vapi.VapiWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VapiWebhookController.class)
@TestPropertySource(properties = "VAPI_WEBHOOK_SECRET=test-webhook-secret")
class VapiWebhookAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VapiWebhookService webhookService;

    @Test
    void webhookRequiresItsSeparateSecret() throws Exception {
        mockMvc.perform(post("/api/webhooks/vapi").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\":\"UNAUTHORIZED\"}"));
        mockMvc.perform(post("/api/webhooks/vapi").header("X-Vapi-Webhook-Secret", "wrong")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/webhooks/vapi").header("X-Vapi-Webhook-Secret", "test-webhook-secret")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        verify(webhookService).handle(null);
    }
}
