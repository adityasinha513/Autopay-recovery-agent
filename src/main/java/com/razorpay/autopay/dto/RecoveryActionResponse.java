package com.razorpay.autopay.dto;

import com.razorpay.autopay.entity.RecoveryAction;

import java.time.LocalDateTime;

public record RecoveryActionResponse(
        Long actionId,
        Long customerId,
        String actionType,
        String status,
        LocalDateTime callbackTime,
        String details,
        LocalDateTime createdAt
) {
    public static RecoveryActionResponse from(RecoveryAction action) {
        return new RecoveryActionResponse(
                action.getId(),
                action.getCustomer().getId(),
                action.getActionType(),
                action.getStatus(),
                action.getCallbackTime(),
                action.getDetails(),
                action.getCreatedAt()
        );
    }
}
