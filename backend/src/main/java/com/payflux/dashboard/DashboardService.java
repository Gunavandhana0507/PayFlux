package com.payflux.dashboard;

import com.payflux.fraud.FraudAnalysis;
import com.payflux.fraud.FraudAnalysisRepository;
import com.payflux.fraud.RiskLevel;
import com.payflux.order.OrderDtos;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentMapper;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStatus;
import com.payflux.refund.Refund;
import com.payflux.refund.RefundRepository;
import com.payflux.refund.RefundStatus;
import com.payflux.security.AuthFacade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final FraudAnalysisRepository fraudRepository;
    private final AuthFacade authFacade;
    private final PaymentMapper paymentMapper;

    public DashboardService(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            FraudAnalysisRepository fraudRepository,
            AuthFacade authFacade,
            PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.fraudRepository = fraudRepository;
        this.authFacade = authFacade;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Summary summary() {
        Long merchantId = authFacade.currentMerchant().getId();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant lastSevenDaysStart = today.minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DashboardDtos.Summary(
                stats(
                        paymentRepository.findByMerchantIdAndCreatedAtBetween(
                                merchantId, todayStart, now),
                        merchantId,
                        todayStart,
                        now),
                stats(
                        paymentRepository.findByMerchantIdAndCreatedAtBetween(
                                merchantId, lastSevenDaysStart, now),
                        merchantId,
                        lastSevenDaysStart,
                        now),
                daily(merchantId, today),
                fraudRepository
                        .findTop5ByPaymentMerchantIdAndRiskLevelInOrderByCreatedAtDesc(
                                merchantId, List.of(RiskLevel.MEDIUM, RiskLevel.HIGH))
                        .stream()
                        .map(this::summary)
                        .toList());
    }

    private DashboardDtos.Stats stats(
            List<Payment> payments, Long merchantId, Instant from, Instant to) {
        List<Payment> captured =
                payments.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).toList();
        long failed =
                payments.stream()
                        .filter(
                                p ->
                                        p.getStatus() == PaymentStatus.FAILED
                                                || p.getStatus() == PaymentStatus.REJECTED)
                        .count();
        BigDecimal refunded =
                refundRepository
                        .findByMerchantIdAndStatus(merchantId, RefundStatus.PROCESSED)
                        .stream()
                        .filter(
                                r ->
                                        !r.getCreatedAt().isBefore(from)
                                                && !r.getCreatedAt().isAfter(to))
                        .map(Refund::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardDtos.Stats(
                payments.size(),
                captured.size(),
                failed,
                captured.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                refunded);
    }

    private List<DashboardDtos.Daily> daily(Long merchantId, LocalDate today) {
        List<DashboardDtos.Daily> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant from = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            List<Payment> payments =
                    paymentRepository.findByMerchantIdAndCreatedAtBetween(merchantId, from, to);
            result.add(
                    new DashboardDtos.Daily(
                            day,
                            payments.stream()
                                    .filter(p -> p.getStatus() == PaymentStatus.CAPTURED)
                                    .count(),
                            payments.stream()
                                    .filter(
                                            p ->
                                                    p.getStatus() == PaymentStatus.FAILED
                                                            || p.getStatus()
                                                                    == PaymentStatus.REJECTED)
                                    .count(),
                            payments.stream()
                                    .filter(p -> p.getStatus() == PaymentStatus.CAPTURED)
                                    .map(Payment::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return result;
    }

    private OrderDtos.PaymentSummaryDto summary(FraudAnalysis analysis) {
        return paymentMapper.toSummary(analysis.getPayment());
    }
}
