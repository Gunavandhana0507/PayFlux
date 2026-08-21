package com.payflux.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FraudAnalysisResponse(
        String id,
        String paymentId,
        int riskScore,
        String riskLevel,
        String decision,
        String prediction,
        String modelVersion,
        List<RiskFactorDto> factors,
        Map<String, String> features,
        Instant createdAt,
        String merchantFeedback,
        String feedbackNote,
        String feedbackByEmail,
        Instant feedbackAt) {}
