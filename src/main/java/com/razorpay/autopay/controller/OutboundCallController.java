package com.razorpay.autopay.controller;

import com.razorpay.autopay.dto.OutboundCallResponse;
import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.integration.vapi.VapiApiException;
import com.razorpay.autopay.integration.vapi.VapiService;
import com.razorpay.autopay.integration.vapi.dto.VapiCallResponse;
import com.razorpay.autopay.service.CallService;
import com.razorpay.autopay.service.CustomerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OutboundCallController {

    private final CustomerService customerService;
    private final CallService callService;
    private final VapiService vapiService;

    public OutboundCallController(CustomerService customerService,
                                  CallService callService,
                                  VapiService vapiService) {
        this.customerService = customerService;
        this.callService = callService;
        this.vapiService = vapiService;
    }

    @PostMapping("/api/calls/outbound/{customerId}")
    public OutboundCallResponse initiateOutboundCall(@PathVariable Long customerId) {
        Customer customer = customerService.getCustomer(customerId);
        Call call = callService.createCall(customerId);

        try {
            VapiCallResponse vapiCall = vapiService.initiateCall(
                    customer.getPhone(), customer.getName(), customerId);
            call = callService.assignVapiCallId(call.getId(), vapiCall.id());
            return new OutboundCallResponse(customerId, call.getId(), vapiCall.id(), call.getStatus());
        } catch (VapiApiException exception) {
            callService.updateCall(call.getId(), CallStatus.FAILED, null);
            throw exception;
        }
    }
}
