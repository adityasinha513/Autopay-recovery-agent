package com.razorpay.autopay.integration.vapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VapiCallResponse(String id, String status) {
}
