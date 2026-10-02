package com.razorpay.autopay.controller;

import com.razorpay.autopay.dto.VapiWebhookAck;
import com.razorpay.autopay.integration.vapi.VapiWebhookService;
import com.razorpay.autopay.integration.vapi.dto.VapiWebhookRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VapiWebhookController {

    private final VapiWebhookService webhookService;

    public VapiWebhookController(VapiWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/api/webhooks/vapi")
    public VapiWebhookAck receiveWebhook(@RequestBody VapiWebhookRequest request) {
        boolean processed = request != null && webhookService.handle(request.message());
        return new VapiWebhookAck(processed);
    }
}
