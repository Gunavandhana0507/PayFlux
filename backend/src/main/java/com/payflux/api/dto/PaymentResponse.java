package com.payflux.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String orderId,
        String orderReceipt,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        BigDecimal refundedAmount,
        String customerEmail,
        String customerName,
        String cardLast4,
        String upiVpa,
        String bankCode,
        String walletProvider,
        String processorReference,
        String failureReason,
        int attemptCount,
        Integer riskScore,
        String riskLevel,
        String merchantFeedback,
        Instant createdAt,
        Instant capturedAt) {}
