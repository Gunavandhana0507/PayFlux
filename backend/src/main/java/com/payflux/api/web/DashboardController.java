package com.payflux.api.web;

import com.payflux.api.domain.Payment;
import com.payflux.api.domain.PaymentStatus;
import com.payflux.api.domain.RiskLevel;
import com.payflux.api.repo.FraudAnalysisRepository;
import com.payflux.api.repo.PaymentRepository;
import com.payflux.api.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregates the merchant's recent activity for the dashboard cards and charts. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final int WINDOW_DAYS = 14;
    private static final int SAMPLE_SIZE = 500;

    private final PaymentRepository paymentRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final CurrentUserService currentUserService;

    public DashboardController(
            PaymentRepository paymentRepository,
            FraudAnalysisRepository fraudAnalysisRepository,
            CurrentUserService currentUserService) {
        this.paymentRepository = paymentRepository;
        this.fraudAnalysisRepository = fraudAnalysisRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        var merchant = currentUserService.requireMerchant();
        List<Payment> payments = paymentRepository
                .findByOrderMerchantIdOrderByCreatedAtDesc(merchant.getId(), PageRequest.of(0, SAMPLE_SIZE))
                .getContent();

        BigDecimal captured = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        long successCount = 0;
        long failedCount = 0;
        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.CAPTURED
                    || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                    || payment.getStatus() == PaymentStatus.REFUNDED) {
                captured = captured.add(payment.getAmount());
                successCount++;
            }
            if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.TIMED_OUT) {
                failedCount++;
            }
            refunded = refunded.add(payment.getRefundedAmount());
        }

        Map<LocalDate, BigDecimal> volumeByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> countByDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = WINDOW_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            volumeByDay.put(day, BigDecimal.ZERO);
            countByDay.put(day, 0L);
        }
        for (Payment payment : payments) {
            LocalDate day = payment.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (!volumeByDay.containsKey(day)) {
                continue;
            }
            countByDay.merge(day, 1L, Long::sum);
            if (payment.getStatus() == PaymentStatus.CAPTURED
                    || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                    || payment.getStatus() == PaymentStatus.REFUNDED) {
                volumeByDay.merge(day, payment.getAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> series = new ArrayList<>();
        volumeByDay.forEach((day, volume) ->
                series.add(Map.of("date", day.toString(), "volume", volume, "count", countByDay.get(day))));

        long flagged = fraudAnalysisRepository
                .findByPaymentOrderMerchantIdAndRiskLevelInOrderByCreatedAtDesc(
                        merchant.getId(), List.of(RiskLevel.MEDIUM, RiskLevel.HIGH), PageRequest.of(0, 1))
                .getTotalElements();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("capturedVolume", captured);
        summary.put("refundedVolume", refunded);
        summary.put("successfulPayments", successCount);
        summary.put("failedPayments", failedCount);
        summary.put("flaggedTransactions", flagged);
        summary.put("totalPayments", (long) payments.size());
        summary.put("series", series);
        return summary;
    }
}
