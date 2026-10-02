package com.razorpay.autopay.tool;

import com.razorpay.autopay.dto.RecoveryActionResponse;
import com.razorpay.autopay.service.RecoveryService;
import org.springframework.stereotype.Component;

@Component
public class SupportTool {

    private final RecoveryService recoveryService;

    public SupportTool(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    public RecoveryActionResponse escalateToSupport(Long customerId, String reason) {
        return RecoveryActionResponse.from(recoveryService.escalateToSupport(customerId, reason));
    }
}
