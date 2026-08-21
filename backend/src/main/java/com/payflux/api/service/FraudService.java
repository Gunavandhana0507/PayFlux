package com.payflux.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflux.api.config.PayFluxProperties;
import com.payflux.api.domain.FraudAnalysis;
import com.payflux.api.domain.MerchantFeedback;
import com.payflux.api.domain.OrderEntity;
import com.payflux.api.domain.Payment;
import com.payflux.api.domain.PaymentStatus;
import com.payflux.api.domain.RiskLevel;
import com.payflux.api.dto.FraudAnalysisResponse;
import com.payflux.api.dto.FraudFeedbackRequest;
import com.payflux.api.dto.RiskFactorDto;
import com.payflux.api.repo.FraudAnalysisRepository;
import com.payflux.api.repo.OrderRepository;
import com.payflux.api.repo.PaymentRepository;
import com.payflux.api.web.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based stand-in for the fraud model service. It returns the same shape a real
 * model would: a score, a level, and the specific factors that drove the score, with
 * the feature snapshot persisted alongside so merchant feedback can be joined back to
 * the exact inputs during a future retraining step.
 */
@Service
public class FraudService {

    private static final String MODEL_VERSION = "rules-v1";

    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PayFluxProperties properties;
    private final ObjectMapper objectMapper;

    public FraudService(
            FraudAnalysisRepository fraudAnalysisRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PayFluxProperties properties,
            ObjectMapper objectMapper) {
        this.fraudAnalysisRepository = fraudAnalysisRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FraudAnalysis analyze(Payment payment) {
        OrderEntity order = payment.getOrder();
        var config = properties.getFraud();

        String customerEmail = order.getCustomerEmail() == null ? "" : order.getCustomerEmail();
        Instant windowStart = Instant.now().minus(config.getFailedAttemptWindowMinutes(), ChronoUnit.MINUTES);
        long failedAttempts = customerEmail.isBlank()
                ? 0
                : paymentRepository.countRecentByCustomerEmailAndStatuses(
                        customerEmail, List.of(PaymentStatus.FAILED, PaymentStatus.TIMED_OUT), windowStart);
        long customerOrderCount = customerEmail.isBlank()
                ? 0
                : orderRepository.countByMerchantIdAndCustomerEmailIgnoreCase(
                        order.getMerchant().getId(), customerEmail);
        String device = payment.getDeviceFingerprint();
        boolean deviceSeenBefore = device != null
                && !device.isBlank()
                && !customerEmail.isBlank()
                && paymentRepository.countByCustomerEmailAndDevice(customerEmail, device, payment.getId()) > 0;

        List<RiskFactorDto> factors = new ArrayList<>();
        int score = 0;

        BigDecimal amount = payment.getAmount();
        if (amount.compareTo(config.getVeryHighAmountThreshold()) > 0) {
            score += 55;
            factors.add(new RiskFactorDto(
                    "AMOUNT_VERY_HIGH",
                    "Unusually high amount for this customer: %s %s is above the %s review threshold"
                            .formatted(
                                    order.getCurrency(),
                                    amount.toPlainString(),
                                    config.getVeryHighAmountThreshold().toPlainString()),
                    55));
        } else if (amount.compareTo(config.getHighAmountThreshold()) > 0) {
            score += 35;
            factors.add(new RiskFactorDto(
                    "AMOUNT_HIGH",
                    "Amount %s %s exceeds the %s high-value threshold"
                            .formatted(
                                    order.getCurrency(),
                                    amount.toPlainString(),
                                    config.getHighAmountThreshold().toPlainString()),
                    35));
        }

        if (failedAttempts >= config.getFailedAttemptThreshold()) {
            score += 30;
            factors.add(new RiskFactorDto(
                    "FAILED_ATTEMPTS",
                    "%d failed attempts in the last %d minutes"
                            .formatted(failedAttempts, config.getFailedAttemptWindowMinutes()),
                    30));
        } else if (failedAttempts > 0) {
            score += 10;
            factors.add(new RiskFactorDto(
                    "RECENT_FAILURE",
                    "%d recent failed attempt(s) by this customer".formatted(failedAttempts),
                    10));
        }

        if (device != null && !device.isBlank() && !deviceSeenBefore) {
            score += 15;
            factors.add(new RiskFactorDto("NEW_DEVICE", "New device - not seen before for this customer", 15));
        }

        if (customerOrderCount <= 1) {
            score += 10;
            factors.add(new RiskFactorDto(
                    "FIRST_TIME_CUSTOMER", "First order from this customer with this merchant", 10));
        }

        int hourOfDay = payment.getCreatedAt().atZone(ZoneOffset.UTC).getHour();
        if (hourOfDay >= 1 && hourOfDay <= 4) {
            score += 5;
            factors.add(new RiskFactorDto("ODD_HOUR", "Payment attempted between 01:00 and 05:00 UTC", 5));
        }

        score = Math.min(score, 100);
        RiskLevel level = score >= config.getHighRiskScore()
                ? RiskLevel.HIGH
                : score >= config.getMediumRiskScore() ? RiskLevel.MEDIUM : RiskLevel.LOW;
        if (factors.isEmpty()) {
            factors.add(new RiskFactorDto("NO_SIGNALS", "No risk signals triggered for this payment", 0));
        }
        String decision = switch (level) {
            case HIGH -> "STEP_UP";
            case MEDIUM -> "REVIEW";
            case LOW -> "ALLOW";
        };

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("amount", amount);
        features.put("currency", order.getCurrency());
        features.put("payment_method", payment.getMethod().name());
        features.put("customer_email", customerEmail);
        features.put("customer_order_count", customerOrderCount);
        features.put("failed_attempts_in_window", failedAttempts);
        features.put("failed_attempt_window_minutes", config.getFailedAttemptWindowMinutes());
        features.put("device_fingerprint", device);
        features.put("device_seen_before", deviceSeenBefore);
        features.put("ip_address", payment.getIpAddress());
        features.put("hour_of_day_utc", hourOfDay);

        FraudAnalysis analysis = fraudAnalysisRepository
                .findByPaymentId(payment.getId())
                .orElseGet(FraudAnalysis::new);
        analysis.setPayment(payment);
        analysis.setRiskScore(score);
        analysis.setRiskLevel(level);
        analysis.setDecision(decision);
        analysis.setModelVersion(MODEL_VERSION);
        analysis.setFactorsJson(writeJson(factors));
        analysis.setFeaturesJson(writeJson(features));
        analysis.setCreatedAt(Instant.now());
        return fraudAnalysisRepository.save(analysis);
    }

    @Transactional
    public FraudAnalysisResponse recordFeedback(
            String merchantId, String analysisId, String merchantEmail, FraudFeedbackRequest request) {
        FraudAnalysis analysis = fraudAnalysisRepository
                .findByIdAndPaymentOrderMerchantId(analysisId, merchantId)
                .orElseGet(() -> fraudAnalysisRepository
                        .findByPaymentId(analysisId)
                        .filter(candidate -> candidate
                                .getPayment()
                                .getOrder()
                                .getMerchant()
                                .getId()
                                .equals(merchantId))
                        .orElseThrow(() -> ApiException.notFound("Fraud alert not found")));

        MerchantFeedback feedback = request.feedback();
        analysis.setMerchantFeedback(feedback);
        analysis.setFeedbackNote(request.note());
        analysis.setFeedbackByEmail(merchantEmail);
        analysis.setFeedbackAt(Instant.now());
        return toResponse(fraudAnalysisRepository.save(analysis));
    }

    @Transactional(readOnly = true)
    public FraudAnalysisResponse toResponse(FraudAnalysis analysis) {
        return new FraudAnalysisResponse(
                analysis.getId(),
                analysis.getPayment().getId(),
                analysis.getRiskScore(),
                analysis.getRiskLevel().name(),
                analysis.getDecision(),
                analysis.getModelVersion(),
                readJson(analysis.getFactorsJson(), new TypeReference<List<RiskFactorDto>>() {}),
                readJson(analysis.getFeaturesJson(), new TypeReference<Map<String, Object>>() {}),
                analysis.getCreatedAt(),
                analysis.getMerchantFeedback() == null
                        ? null
                        : analysis.getMerchantFeedback().name(),
                analysis.getFeedbackNote(),
                analysis.getFeedbackByEmail(),
                analysis.getFeedbackAt());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize fraud analysis payload", ex);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to read fraud analysis payload", ex);
        }
    }
}
