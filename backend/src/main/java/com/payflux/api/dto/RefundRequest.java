package com.payflux.api.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** A null {@code amount} means a full refund of the remaining refundable balance. */
public record RefundRequest(@DecimalMin(value = "0.01") BigDecimal amount, String reason) {}
