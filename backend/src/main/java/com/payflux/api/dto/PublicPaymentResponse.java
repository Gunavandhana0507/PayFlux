package com.payflux.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Checkout-facing payment view. {@code nextAction} is OTP_VERIFICATION when the risk
 * engine asked for a step-up, and null otherwise.
 */
public record PublicPaymentResponse(
        String id,
        String orderId,
        String status,
        String method,
        BigDecimal amount,
        String currency,
        String nextAction,
        String message,
        String failureReason,
        Instant createdAt) {}
