package com.payflux.fraud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.IdGenerator;
import com.payflux.common.PageResponse;
import com.payflux.order.OrderDtos;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentMapper;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStatus;
import com.payflux.security.AuthFacade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class FraudService {
    private final PaymentRepository paymentRepository;
    private final FraudAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;
    private final AuthFacade authFacade;
    private final BigDecimal highAmountThreshold;
    private final BigDecimal veryHighAmountThreshold;

    public FraudService(
            PaymentRepository paymentRepository,
            FraudAnalysisRepository analysisRepository,
            ObjectMapper objectMapper,
            PaymentMapper paymentMapper,
            AuthFacade authFacade,
            @Value("${payflux.fraud.high-amount-threshold}") BigDecimal highAmountThreshold,
            @Value("${payflux.fraud.very-high-amount-threshold}")
                    BigDecimal veryHighAmountThreshold) {
        this.paymentRepository = paymentRepository;
        this.analysisRepository = analysisRepository;
        this.objectMapper = objectMapper;
        this.paymentMapper = paymentMapper;
        this.authFacade = authFacade;
        this.highAmountThreshold = highAmountThreshold;
        this.veryHighAmountThreshold = veryHighAmountThreshold;
    }

    @Transactional
    public FraudAssessment assess(Payment payment) {
        Instant since = Instant.now().minusSeconds(600);
        List<Payment> history =
                paymentRepository.findByMerchantIdAndCustomerEmailAndCreatedAtBefore(
                        payment.getMerchant().getId(),
                        payment.getCustomerEmail(),
                        payment.getCreatedAt());
        List<Payment> recent =
                paymentRepository
                        .findByMerchantIdAndCustomerEmailAndCreatedAtAfter(
                                payment.getMerchant().getId(), payment.getCustomerEmail(), since)
                        .stream()
                        .filter(p -> !p.getId().equals(payment.getId()))
                        .toList();
        List<Payment> captured =
                history.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).toList();
        BigDecimal average =
                captured.isEmpty()
                        ? null
                        : captured.stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(
                                        BigDecimal.valueOf(captured.size()),
                                        4,
                                        RoundingMode.HALF_UP);
        int failed =
                (int)
                        recent.stream()
                                .filter(
                                        p ->
                                                p.getStatus() == PaymentStatus.FAILED
                                                        || p.getStatus() == PaymentStatus.REJECTED)
                                .count();
        boolean newDevice =
                history.stream()
                        .noneMatch(p -> Objects.equals(p.getDeviceId(), payment.getDeviceId()));
        boolean unusual =
                (average != null
                                && payment.getAmount()
                                                .compareTo(average.multiply(BigDecimal.valueOf(4)))
                                        > 0)
                        || payment.getAmount().compareTo(veryHighAmountThreshold) > 0;
        FraudDtos.FraudFeatures features =
                new FraudDtos.FraudFeatures(
                        payment.getAmount(),
                        average,
                        recent.size(),
                        failed,
                        newDevice,
                        newDevice ? new BigDecimal("0.6") : new BigDecimal("0.1"),
                        new BigDecimal("0.1"),
                        unusual);
        List<FraudDtos.FraudFactor> factors = new ArrayList<>();
        if (payment.getAmount().compareTo(highAmountThreshold) > 0)
            factors.add(
                    new FraudDtos.FraudFactor(
                            "HIGH_AMOUNT",
                            "Amount ₹"
                                    + payment.getAmount().stripTrailingZeros().toPlainString()
                                    + " is above the ₹50,000 high-value threshold",
                            new BigDecimal("0.45")));
        if (payment.getAmount().compareTo(veryHighAmountThreshold) > 0)
            factors.add(
                    new FraudDtos.FraudFactor(
                            "VERY_HIGH_AMOUNT",
                            "Amount is more than double the high-value threshold",
                            new BigDecimal("0.30")));
        if (average != null
                && payment.getAmount().compareTo(average.multiply(BigDecimal.valueOf(4))) > 0) {
            BigDecimal ratio = payment.getAmount().divide(average, 1, RoundingMode.HALF_UP);
            factors.add(
                    new FraudDtos.FraudFactor(
                            "ABOVE_CUSTOMER_AVERAGE",
                            "Amount is "
                                    + ratio.toPlainString()
                                    + "x this customer's usual payment of ₹"
                                    + average.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            new BigDecimal("0.30")));
        }
        if (failed >= 3)
            factors.add(
                    new FraudDtos.FraudFactor(
                            "REPEATED_FAILURES",
                            failed + " failed attempts in the last 10 minutes",
                            new BigDecimal("0.35")));
        if (recent.size() >= 5)
            factors.add(
                    new FraudDtos.FraudFactor(
                            "HIGH_FREQUENCY",
                            recent.size() + " payment attempts in the last 10 minutes",
                            new BigDecimal("0.15")));
        if (newDevice && payment.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0)
            factors.add(
                    new FraudDtos.FraudFactor(
                            "NEW_DEVICE",
                            "Payment from a device this customer hasn't used before",
                            new BigDecimal("0.15")));
        BigDecimal score =
                factors.stream()
                        .map(FraudDtos.FraudFactor::weight)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .min(BigDecimal.ONE)
                        .setScale(4, RoundingMode.HALF_UP);
        RiskLevel level =
                score.compareTo(new BigDecimal("0.70")) >= 0
                        ? RiskLevel.HIGH
                        : score.compareTo(new BigDecimal("0.40")) >= 0
                                ? RiskLevel.MEDIUM
                                : RiskLevel.LOW;
        FraudAnalysis analysis = new FraudAnalysis();
        analysis.setId(IdGenerator.next("fa_"));
        analysis.setPayment(payment);
        analysis.setRiskScore(score);
        analysis.setRiskLevel(level);
        analysis.setPrediction(
                level == RiskLevel.HIGH
                        ? Prediction.LIKELY_FRAUD
                        : level == RiskLevel.MEDIUM
                                ? Prediction.SUSPICIOUS
                                : Prediction.LEGITIMATE);
        analysis.setModelVersion("rules-v1");
        analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
        analysis.setCreatedAt(Instant.now());
        try {
            analysis.setFeaturesJson(objectMapper.writeValueAsString(features));
            analysis.setFactorsJson(objectMapper.writeValueAsString(factors));
        } catch (Exception e) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }
        analysisRepository.save(analysis);
        return new FraudAssessment(analysis, features, factors);
    }

    public FraudDtos.FraudAnalysisDto toDto(FraudAnalysis analysis) {
        try {
            return new FraudDtos.FraudAnalysisDto(
                    analysis.getId(),
                    analysis.getPayment().getId(),
                    analysis.getRiskScore(),
                    analysis.getRiskLevel(),
                    analysis.getPrediction(),
                    analysis.getModelVersion(),
                    analysis.getAnalysisStatus(),
                    objectMapper.readValue(analysis.getFactorsJson(), new TypeReference<>() {}),
                    objectMapper.readValue(
                            analysis.getFeaturesJson(), FraudDtos.FraudFeatures.class),
                    analysis.getMerchantFeedback(),
                    analysis.getFeedbackAt(),
                    analysis.getFeedbackNote(),
                    analysis.getCreatedAt());
        } catch (Exception e) {
            return new FraudDtos.FraudAnalysisDto(
                    analysis.getId(),
                    analysis.getPayment().getId(),
                    analysis.getRiskScore(),
                    analysis.getRiskLevel(),
                    analysis.getPrediction(),
                    analysis.getModelVersion(),
                    analysis.getAnalysisStatus(),
                    List.of(),
                    null,
                    analysis.getMerchantFeedback(),
                    analysis.getFeedbackAt(),
                    analysis.getFeedbackNote(),
                    analysis.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.PaymentSummaryDto> listAlerts(int page, int size) {
        var result =
                analysisRepository.findByPaymentMerchantIdAndRiskLevelIn(
                        authFacade.currentMerchant().getId(),
                        List.of(RiskLevel.MEDIUM, RiskLevel.HIGH),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.getContent().stream()
                        .map(analysis -> paymentMapper.toSummary(analysis.getPayment()))
                        .toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Transactional
    public FraudDtos.FraudAnalysisDto submitFeedback(String id, FraudDtos.FeedbackRequest request) {
        FraudAnalysis analysis =
                analysisRepository
                        .findById(id)
                        .filter(
                                value ->
                                        value.getPayment()
                                                .getMerchant()
                                                .getId()
                                                .equals(authFacade.currentMerchant().getId()))
                        .orElseThrow(() -> new NotFoundException("Fraud analysis was not found"));
        analysis.setMerchantFeedback(request.feedback());
        analysis.setFeedbackNote(request.note());
        analysis.setFeedbackAt(Instant.now());
        analysis.setFeedbackByUserId(authFacade.currentUserId());
        return toDto(analysisRepository.save(analysis));
    }

    public record FraudAssessment(
            FraudAnalysis analysis,
            FraudDtos.FraudFeatures features,
            List<FraudDtos.FraudFactor> factors) {}
}
