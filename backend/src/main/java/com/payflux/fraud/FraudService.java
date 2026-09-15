package com.payflux.fraud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflux.common.IdGenerator;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudService {
    private final PaymentRepository paymentRepository;
    private final FraudAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal highAmountThreshold;
    private final BigDecimal veryHighAmountThreshold;
    public FraudService(PaymentRepository paymentRepository, FraudAnalysisRepository analysisRepository, ObjectMapper objectMapper, @Value("${payflux.fraud.high-amount-threshold}") BigDecimal highAmountThreshold, @Value("${payflux.fraud.very-high-amount-threshold}") BigDecimal veryHighAmountThreshold) { this.paymentRepository = paymentRepository; this.analysisRepository = analysisRepository; this.objectMapper = objectMapper; this.highAmountThreshold = highAmountThreshold; this.veryHighAmountThreshold = veryHighAmountThreshold; }
    @Transactional
    public FraudAssessment assess(Payment payment) {
        Instant since = Instant.now().minusSeconds(600);
        List<Payment> prior = paymentRepository.findByMerchantIdAndCustomerEmailAndCreatedAtAfter(payment.getMerchant().getId(), payment.getCustomerEmail(), since).stream().filter(p -> !p.getId().equals(payment.getId())).toList();
        List<Payment> captured = prior.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).toList();
        BigDecimal average = captured.isEmpty() ? null : captured.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(captured.size()), 4, RoundingMode.HALF_UP);
        int failed = (int) prior.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED || p.getStatus() == PaymentStatus.REJECTED).count();
        boolean newDevice = prior.stream().noneMatch(p -> p.getDeviceId().equals(payment.getDeviceId()));
        boolean unusual = (average != null && payment.getAmount().compareTo(average.multiply(BigDecimal.valueOf(4))) > 0) || payment.getAmount().compareTo(veryHighAmountThreshold) > 0;
        FraudDtos.FraudFeatures features = new FraudDtos.FraudFeatures(payment.getAmount(), average, prior.size(), failed, newDevice, newDevice ? new BigDecimal("0.6") : new BigDecimal("0.1"), new BigDecimal("0.1"), unusual);
        List<FraudDtos.FraudFactor> factors = new ArrayList<>();
        if (payment.getAmount().compareTo(highAmountThreshold) > 0) factors.add(new FraudDtos.FraudFactor("HIGH_AMOUNT", "Amount ₹" + payment.getAmount().stripTrailingZeros().toPlainString() + " is above the ₹50,000 high-value threshold", new BigDecimal("0.45")));
        if (payment.getAmount().compareTo(veryHighAmountThreshold) > 0) factors.add(new FraudDtos.FraudFactor("VERY_HIGH_AMOUNT", "Amount is more than double the high-value threshold", new BigDecimal("0.30")));
        if (average != null && payment.getAmount().compareTo(average.multiply(BigDecimal.valueOf(4))) > 0) { BigDecimal ratio = payment.getAmount().divide(average, 1, RoundingMode.HALF_UP); factors.add(new FraudDtos.FraudFactor("ABOVE_CUSTOMER_AVERAGE", "Amount is " + ratio.toPlainString() + "x this customer's usual payment of ₹" + average.setScale(2, RoundingMode.HALF_UP).toPlainString(), new BigDecimal("0.30"))); }
        if (failed >= 3) factors.add(new FraudDtos.FraudFactor("REPEATED_FAILURES", failed + " failed attempts in the last 10 minutes", new BigDecimal("0.35")));
        if (prior.size() >= 5) factors.add(new FraudDtos.FraudFactor("HIGH_FREQUENCY", prior.size() + " payment attempts in the last 10 minutes", new BigDecimal("0.15")));
        if (newDevice && payment.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) factors.add(new FraudDtos.FraudFactor("NEW_DEVICE", "Payment from a device this customer hasn't used before", new BigDecimal("0.15")));
        BigDecimal score = factors.stream().map(FraudDtos.FraudFactor::weight).reduce(BigDecimal.ZERO, BigDecimal::add).min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
        RiskLevel level = score.compareTo(new BigDecimal("0.70")) >= 0 ? RiskLevel.HIGH : score.compareTo(new BigDecimal("0.40")) >= 0 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        FraudAnalysis analysis = new FraudAnalysis(); analysis.setId(IdGenerator.next("fa_")); analysis.setPayment(payment); analysis.setRiskScore(score); analysis.setRiskLevel(level); analysis.setPrediction(level == RiskLevel.HIGH ? Prediction.LIKELY_FRAUD : level == RiskLevel.MEDIUM ? Prediction.SUSPICIOUS : Prediction.LEGITIMATE); analysis.setModelVersion("rules-v1"); analysis.setAnalysisStatus(AnalysisStatus.COMPLETED); analysis.setCreatedAt(Instant.now());
        try { analysis.setFeaturesJson(objectMapper.writeValueAsString(features)); analysis.setFactorsJson(objectMapper.writeValueAsString(factors)); } catch (Exception e) { analysis.setAnalysisStatus(AnalysisStatus.FAILED); }
        analysisRepository.save(analysis);
        return new FraudAssessment(analysis, features, factors);
    }
    public FraudDtos.FraudAnalysisDto toDto(FraudAnalysis analysis) {
        try { return new FraudDtos.FraudAnalysisDto(analysis.getId(), analysis.getPayment().getId(), analysis.getRiskScore(), analysis.getRiskLevel(), analysis.getPrediction(), analysis.getModelVersion(), analysis.getAnalysisStatus(), objectMapper.readValue(analysis.getFactorsJson(), new TypeReference<>() {}), objectMapper.readValue(analysis.getFeaturesJson(), FraudDtos.FraudFeatures.class), analysis.getMerchantFeedback(), analysis.getFeedbackAt(), analysis.getFeedbackNote(), analysis.getCreatedAt()); }
        catch (Exception e) { return new FraudDtos.FraudAnalysisDto(analysis.getId(), analysis.getPayment().getId(), analysis.getRiskScore(), analysis.getRiskLevel(), analysis.getPrediction(), analysis.getModelVersion(), analysis.getAnalysisStatus(), List.of(), null, analysis.getMerchantFeedback(), analysis.getFeedbackAt(), analysis.getFeedbackNote(), analysis.getCreatedAt()); }
    }
    public record FraudAssessment(FraudAnalysis analysis, FraudDtos.FraudFeatures features, List<FraudDtos.FraudFactor> factors) {}
}
