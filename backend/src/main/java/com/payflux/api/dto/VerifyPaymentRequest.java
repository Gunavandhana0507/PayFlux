package com.payflux.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(@NotBlank String otp) {}
