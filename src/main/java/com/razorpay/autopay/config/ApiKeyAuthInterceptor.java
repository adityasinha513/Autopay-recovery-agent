package com.razorpay.autopay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final String WEBHOOK_PATH = "/api/webhooks/vapi";
    private final String apiKey;
    private final String webhookSecret;

    public ApiKeyAuthInterceptor(@Value("${security.api-key:}") String apiKey,
                                 @Value("${security.vapi-webhook-secret:}") String webhookSecret) {
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || "/api/health".equals(request.getRequestURI())) {
            return true;
        }

        boolean webhook = WEBHOOK_PATH.equals(request.getRequestURI());
        String expected = webhook ? webhookSecret : apiKey;
        String supplied = request.getHeader(webhook ? "X-Vapi-Webhook-Secret" : "X-API-Key");
        if (StringUtils.hasText(expected) && StringUtils.hasText(supplied)
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid API credential is required.\"}");
        return false;
    }
}
