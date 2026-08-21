package com.payflux.api.dto;

import com.payflux.api.domain.OrderEntity;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String merchantId,
        BigDecimal amount,
        String currency,
        String receipt,
        String description,
        String notes,
        String customerName,
        String customerEmail,
        String customerPhone,
        String status,
        String idempotencyKey,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt,
        String paymentUrl) {

    public static OrderResponse from(OrderEntity order, String checkoutBaseUrl) {
        return new OrderResponse(
                order.getId(),
                order.getMerchant().getId(),
                order.getAmount(),
                order.getCurrency(),
                order.getReceipt(),
                order.getDescription(),
                order.getNotes(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getStatus().name(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getPaidAt(),
                checkoutBaseUrl + "/pay/" + order.getId());
    }
}
