package com.razorpay.autopay.integration.vapi;

import com.razorpay.autopay.integration.vapi.dto.VapiAssistantOverrides;
import com.razorpay.autopay.integration.vapi.dto.VapiCallResponse;
import com.razorpay.autopay.integration.vapi.dto.VapiCreateCallRequest;
import com.razorpay.autopay.integration.vapi.dto.VapiCustomerRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class VapiService {

    private final VapiProperties properties;
    private final RestClient restClient;

    public VapiService(VapiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public VapiCallResponse initiateCall(String customerPhone, String customerName, Long customerId) {
        requireConfigured(properties.apiKey(), "VAPI_API_KEY");
        requireConfigured(properties.assistantId(), "VAPI_ASSISTANT_ID");
        requireConfigured(properties.phoneNumberId(), "VAPI_PHONE_NUMBER_ID");

        VapiCreateCallRequest request = new VapiCreateCallRequest(
                properties.assistantId(),
                properties.phoneNumberId(),
                new VapiCustomerRequest(customerPhone, customerName),
                Map.of("autopayCustomerId", customerId),
                new VapiAssistantOverrides(Map.of("autopayCustomerId", customerId))
        );

        VapiCallResponse response = execute(() -> restClient.post()
                .uri("/call")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .body(request)
                .retrieve()
                .body(VapiCallResponse.class));

        if (response == null || !StringUtils.hasText(response.id())) {
            throw new VapiApiException(HttpStatus.BAD_GATEWAY,
                    "Vapi accepted the call request but returned no call ID for customer " + customerId);
        }
        return response;
    }

    public VapiCallResponse getCall(String callId) {
        requireConfigured(properties.apiKey(), "VAPI_API_KEY");
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be blank");
        }

        return execute(() -> restClient.get()
                .uri("/call/{callId}", callId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .retrieve()
                .body(VapiCallResponse.class));
    }

    private <T> T execute(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            throw new VapiApiException(HttpStatus.BAD_GATEWAY,
                    "Vapi API returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new VapiApiException(HttpStatus.BAD_GATEWAY,
                    "Could not communicate with the Vapi API", exception);
        }
    }

    private void requireConfigured(String value, String variableName) {
        if (!StringUtils.hasText(value)) {
            throw new VapiApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    variableName + " is not configured");
        }
    }
}
