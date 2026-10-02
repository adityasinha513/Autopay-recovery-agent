package com.razorpay.autopay.tool;

import com.razorpay.autopay.dto.RecoveryActionResponse;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.service.RecoveryService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RecoveryTool {

    private final RecoveryService recoveryService;

    public RecoveryTool(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    public RecoveryActionResponse scheduleCallback(Long customerId, LocalDateTime callbackTime) {
        return RecoveryActionResponse.from(recoveryService.scheduleCallback(customerId, callbackTime));
    }

    public RecoveryActionResponse recordRecoveryOutcome(Long customerId, RecoveryOutcome outcome) {
        return RecoveryActionResponse.from(recoveryService.recordOutcome(customerId, outcome));
    }
}
