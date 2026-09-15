package com.payflux.fraud;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class FraudDtos {
    private FraudDtos() {}
    public record FraudFactor(String code, String description, BigDecimal weight) {}
    public record FraudFeatures(BigDecimal amount, BigDecimal customerAvgAmount, int attemptsLast10Min, int failedAttemptsLast10Min, boolean newDevice, BigDecimal deviceRiskScore, BigDecimal locationRiskScore, boolean unusualTransaction) {}
    public record FraudAnalysisDto(String id, String paymentId, BigDecimal riskScore, RiskLevel riskLevel, Prediction prediction, String modelVersion, AnalysisStatus analysisStatus, List<FraudFactor> factors, FraudFeatures features, MerchantFeedback merchantFeedback, Instant feedbackAt, String feedbackNote, Instant createdAt) {}
    public record FeedbackRequest(MerchantFeedback feedback, @Size(max = 500) String note) {}
}
