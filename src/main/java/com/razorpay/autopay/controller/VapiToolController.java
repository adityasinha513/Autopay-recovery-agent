package com.razorpay.autopay.controller;

import com.razorpay.autopay.enums.RecoveryOutcome;
import com.razorpay.autopay.exception.CustomerNotFoundException;
import com.razorpay.autopay.tool.CustomerTool;
import com.razorpay.autopay.tool.PaymentTool;
import com.razorpay.autopay.tool.RecoveryTool;
import com.razorpay.autopay.tool.SupportTool;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/tools/vapi")
public class VapiToolController {

    private final CustomerTool customerTool;
    private final PaymentTool paymentTool;
    private final RecoveryTool recoveryTool;
    private final SupportTool supportTool;

    public VapiToolController(CustomerTool customerTool,
                              PaymentTool paymentTool,
                              RecoveryTool recoveryTool,
                              SupportTool supportTool) {
        this.customerTool = customerTool;
        this.paymentTool = paymentTool;
        this.recoveryTool = recoveryTool;
        this.supportTool = supportTool;
    }

    @PostMapping("/{toolName}")
    public ResponseEntity<?> invoke(@PathVariable String toolName,
                                    @RequestBody(required = false) Map<String, Object> arguments) {
        try {
            if (arguments == null) {
                throw new InvalidToolArgumentsException("Request body must be a JSON object.");
            }

            Object result = switch (toolName) {
                case "get_customer_details" -> customerTool.getCustomerDetails(customerId(arguments));
                case "check_payment_status" -> paymentTool.checkPaymentStatus(customerId(arguments));
                case "retry_payment" -> paymentTool.retryPayment(customerId(arguments));
                case "send_payment_link" -> paymentTool.sendPaymentLink(customerId(arguments));
                case "schedule_callback" -> recoveryTool.scheduleCallback(
                        customerId(arguments), callbackTime(arguments));
                case "record_recovery_outcome" -> recoveryTool.recordRecoveryOutcome(
                        customerId(arguments), recoveryOutcome(arguments));
                case "escalate_to_support" -> supportTool.escalateToSupport(
                        customerId(arguments), requiredString(arguments, "reason"));
                default -> throw new UnknownVapiToolException(toolName);
            };
            return ResponseEntity.ok(result);
        } catch (UnknownVapiToolException exception) {
            return error(HttpStatus.BAD_REQUEST, "UNKNOWN_TOOL", exception.getMessage());
        } catch (InvalidToolArgumentsException exception) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENTS", exception.getMessage());
        } catch (CustomerNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", exception.getMessage());
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "TOOL_EXECUTION_FAILED",
                    "The requested tool could not be completed.");
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ToolErrorResponse> handleMalformedJson() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENTS", "Request body must be a valid JSON object.");
    }

    private static Long customerId(Map<String, Object> arguments) {
        Object value = arguments.get("customerId");
        if (!(value instanceof Number number)) {
            throw new InvalidToolArgumentsException("customerId is required and must be a positive integer.");
        }
        try {
            long customerId = new BigDecimal(number.toString()).longValueExact();
            if (customerId <= 0) {
                throw new ArithmeticException("customerId must be positive");
            }
            return customerId;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new InvalidToolArgumentsException("customerId is required and must be a positive integer.");
        }
    }

    private static String requiredString(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new InvalidToolArgumentsException(name + " is required and must be a non-empty string.");
        }
        return string.trim();
    }

    private static LocalDateTime callbackTime(Map<String, Object> arguments) {
        String value = requiredString(arguments, "callbackTime");
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new InvalidToolArgumentsException("callbackTime must be an ISO-8601 local date-time.");
        }
    }

    private static RecoveryOutcome recoveryOutcome(Map<String, Object> arguments) {
        String value = requiredString(arguments, "outcome");
        try {
            return RecoveryOutcome.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidToolArgumentsException("outcome must be a supported recovery outcome.");
        }
    }

    private static ResponseEntity<ToolErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ToolErrorResponse(code, message));
    }

    private record ToolErrorResponse(String code, String message) {
    }

    private static final class InvalidToolArgumentsException extends RuntimeException {
        private InvalidToolArgumentsException(String message) {
            super(message);
        }
    }

    private static final class UnknownVapiToolException extends RuntimeException {
        private UnknownVapiToolException(String toolName) {
            super("Unsupported Vapi tool: " + toolName);
        }
    }
}
