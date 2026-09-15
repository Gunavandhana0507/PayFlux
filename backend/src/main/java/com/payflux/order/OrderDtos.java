package com.payflux.order;

import com.payflux.fraud.RiskLevel;
import com.payflux.payment.PaymentMethod;
import com.payflux.payment.PaymentStatus;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2)
                    BigDecimal amount,
            @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Size(max = 500) String notes,
            @Email String customerEmail,
            @Min(1) @Max(1440) Integer expiresInMinutes) {}

    public record OrderDto(
            String id,
            BigDecimal amount,
            String currency,
            String notes,
            String customerEmail,
            OrderStatus status,
            Instant expiresAt,
            Instant createdAt,
            String paymentUrl,
            String latestPaymentId,
            PaymentStatus latestPaymentStatus) {}

    public record OrderDetailDto(OrderDto order, List<PaymentSummaryDto> payments) {}

    public record PaymentSummaryDto(
            String id,
            String orderId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String methodSummary,
            PaymentStatus status,
            RiskLevel riskLevel,
            BigDecimal riskScore,
            String customerEmail,
            BigDecimal refundedAmount,
            Instant createdAt) {}

    public record PublicOrderDto(
            String id,
            String merchantName,
            BigDecimal amount,
            String currency,
            String notes,
            OrderStatus status,
            Instant expiresAt,
            long secondsRemaining,
            boolean payableNow,
            PublicPaymentDto latestPayment) {}

    public record PublicPaymentDto(
            String id,
            String orderId,
            PaymentStatus status,
            PaymentMethod method,
            String methodSummary,
            BigDecimal amount,
            String currency,
            String failureReason,
            boolean verificationRequired,
            Instant createdAt) {}
}
