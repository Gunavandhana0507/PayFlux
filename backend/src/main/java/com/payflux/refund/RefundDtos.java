package com.payflux.refund;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class RefundDtos {
    private RefundDtos() {}
    public record CreateRefundRequest(@DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount, @Size(max = 500) String reason) {}
    public record RefundDto(String id, String paymentId, String orderId, BigDecimal amount, String currency, RefundStatus status, String reason, String failureReason, Instant createdAt, Instant processedAt) {}
}
