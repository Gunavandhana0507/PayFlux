package com.payflux.api.dto;

import com.payflux.api.domain.Refund;
import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        String id,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        String processorReference,
        String initiatedBy,
        Instant createdAt,
        Instant processedAt) {

    public static RefundResponse from(Refund refund) {
        var payment = refund.getPayment();
        return new RefundResponse(
                refund.getId(),
                payment.getId(),
                payment.getOrder().getId(),
                refund.getAmount(),
                payment.getOrder().getCurrency(),
                refund.getStatus().name(),
                refund.getReason(),
                refund.getProcessorReference(),
                refund.getInitiatedBy(),
                refund.getCreatedAt(),
                refund.getProcessedAt());
    }
}
