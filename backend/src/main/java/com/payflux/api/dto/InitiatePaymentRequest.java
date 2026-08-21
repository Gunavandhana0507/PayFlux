package com.payflux.api.dto;

import com.payflux.api.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Checkout submission. Only the fields relevant to the chosen method are read;
 * {@code simulateOutcome} lets the caller force a mock processor result
 * (SUCCESS / FAILURE / TIMEOUT) instead of the configured default.
 */
public record InitiatePaymentRequest(
        @NotBlank String orderId,
        @NotNull PaymentMethod method,
        String cardNumber,
        String cardHolderName,
        String cardExpiry,
        String cardCvv,
        String upiVpa,
        String bankCode,
        String walletProvider,
        String deviceFingerprint,
        String simulateOutcome) {}
