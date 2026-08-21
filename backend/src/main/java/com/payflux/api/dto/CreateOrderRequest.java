package com.payflux.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        String receipt,
        String description,
        String customerName,
        @Email String customerEmail,
        String customerPhone) {}
