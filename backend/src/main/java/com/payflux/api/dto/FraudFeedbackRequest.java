package com.payflux.api.dto;

import com.payflux.api.domain.MerchantFeedback;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FraudFeedbackRequest(@NotNull MerchantFeedback feedback, @Size(max = 512) String note) {}
