package com.payflux.api.dto;

import com.payflux.api.domain.OrderEntity;
import java.math.BigDecimal;
import java.time.Instant;

/** Order view exposed on the public checkout page - no merchant KYC data. */
public record PublicOrderResponse(
        String id,
        String merchantName,
        BigDecimal amount,
        String currency,
        String description,
        String customerName,
        String customerEmail,
        String status,
        Instant expiresAt,
        boolean payable) {

    public static PublicOrderResponse from(OrderEntity order) {
        return new PublicOrderResponse(
                order.getId(),
                order.getMerchant().getBusinessName(),
                order.getAmount(),
                order.getCurrency(),
                order.getDescription(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getExpiresAt(),
                order.isPayable());
    }
}
