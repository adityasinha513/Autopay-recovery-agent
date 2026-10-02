package com.razorpay.autopay.controller;

import com.razorpay.autopay.dto.CallSummaryResponse;
import com.razorpay.autopay.dto.DashboardMetricsResponse;
import com.razorpay.autopay.dto.RecoveryActionResponse;
import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.entity.RecoveryAction;
import com.razorpay.autopay.enums.CallStatus;
import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.repository.CallRepository;
import com.razorpay.autopay.repository.RecoveryActionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DashboardController {

    private final CallRepository callRepository;
    private final RecoveryActionRepository recoveryActionRepository;

    public DashboardController(CallRepository callRepository,
                               RecoveryActionRepository recoveryActionRepository) {
        this.callRepository = callRepository;
        this.recoveryActionRepository = recoveryActionRepository;
    }

    @GetMapping("/api/calls")
    public List<CallSummaryResponse> getCalls() {
        return callRepository.findAllByOrderByStartedAtDesc().stream()
                .map(CallSummaryResponse::from)
                .toList();
    }

    @GetMapping("/api/recovery-actions")
    public List<RecoveryActionResponse> getRecoveryActions() {
        return recoveryActionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RecoveryActionResponse::from)
                .toList();
    }

    @GetMapping("/api/metrics")
    public DashboardMetricsResponse getMetrics() {
        List<Call> calls = callRepository.findAll();
        List<RecoveryAction> actions = recoveryActionRepository.findAll();

        long totalCalls = calls.size();
        long completedCalls = calls.stream()
                .filter(call -> call.getStatus() == CallStatus.COMPLETED)
                .count();
        long recoveredCalls = calls.stream()
                .filter(call -> call.getStatus() == CallStatus.COMPLETED
                        && call.getOutcome() == RecoveryOutcome.RECOVERED)
                .count();
        long averageDuration = Math.round(calls.stream()
                .filter(call -> call.getDurationSeconds() != null)
                .mapToLong(Call::getDurationSeconds)
                .average()
                .orElse(0));
        double recoveryRate = completedCalls == 0
                ? 0.0
                : Math.round((double) recoveredCalls / completedCalls * 1000.0) / 10.0;
        long paymentLinksSent = actions.stream()
                .filter(action -> "PAYMENT_LINK_SENT".equals(action.getStatus()))
                .count();
        long escalations = actions.stream()
                .filter(action -> "SUPPORT_ESCALATION".equals(action.getActionType()))
                .count();

        return new DashboardMetricsResponse(totalCalls, completedCalls, recoveryRate,
                averageDuration, paymentLinksSent, escalations);
    }
}
