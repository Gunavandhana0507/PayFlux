package com.payflux.dashboard;

import com.payflux.fraud.FraudAnalysisRepository;
import com.payflux.fraud.RiskLevel;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStatus;
import com.payflux.refund.Refund;
import com.payflux.refund.RefundRepository;
import com.payflux.refund.RefundStatus;
import com.payflux.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class DashboardService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final FraudAnalysisRepository fraudRepository;
    private final AuthFacade authFacade;
    public DashboardService(PaymentRepository paymentRepository, RefundRepository refundRepository, FraudAnalysisRepository fraudRepository, AuthFacade authFacade) { this.paymentRepository = paymentRepository; this.refundRepository = refundRepository; this.fraudRepository = fraudRepository; this.authFacade = authFacade; }
    @Transactional(readOnly = true)
    public DashboardDtos.Summary summary() {
        Long merchantId = authFacade.currentMerchant().getId(); Instant now = Instant.now(); ZoneId zone = ZoneId.systemDefault(); LocalDate today = LocalDate.now(zone); Instant todayStart = today.atStartOfDay(zone).toInstant();
        return new DashboardDtos.Summary(stats(paymentRepository.findByMerchantIdAndCreatedAtBetween(merchantId, todayStart, now), merchantId, todayStart, now), stats(paymentRepository.findByMerchantIdAndCreatedAtAfterAndStatusIn(merchantId, now.minus(Duration.ofDays(7)), List.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.REJECTED)), merchantId, now.minus(Duration.ofDays(7)), now), daily(merchantId, today, zone), fraudRepository.findTop5ByPaymentMerchantIdAndRiskLevelInOrderByCreatedAtDesc(merchantId, List.of(RiskLevel.MEDIUM, RiskLevel.HIGH)).stream().map(this::summary).toList());
    }
    private DashboardDtos.Stats stats(List<Payment> payments, Long merchantId, Instant from, Instant to) { List<Payment> captured = payments.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).toList(); long failed = payments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED || p.getStatus() == PaymentStatus.REJECTED).count(); BigDecimal refunded = refundRepository.findByMerchantIdAndStatus(merchantId, RefundStatus.PROCESSED).stream().filter(r -> !r.getCreatedAt().isBefore(from) && !r.getCreatedAt().isAfter(to)).map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); return new DashboardDtos.Stats(payments.size(), captured.size(), failed, captured.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add), refunded); }
    private List<DashboardDtos.Daily> daily(Long merchantId, LocalDate today, ZoneId zone) { List<DashboardDtos.Daily> result = new ArrayList<>(); for (int i = 6; i >= 0; i--) { LocalDate day = today.minusDays(i); Instant from = day.atStartOfDay(zone).toInstant(); Instant to = day.plusDays(1).atStartOfDay(zone).toInstant(); List<Payment> payments = paymentRepository.findByMerchantIdAndCreatedAtBetween(merchantId, from, to); result.add(new DashboardDtos.Daily(day, payments.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).count(), payments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED || p.getStatus() == PaymentStatus.REJECTED).count(), payments.stream().filter(p -> p.getStatus() == PaymentStatus.CAPTURED).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))); } return result; }
    private com.payflux.order.OrderDtos.PaymentSummaryDto summary(com.payflux.fraud.FraudAnalysis a) { Payment p = a.getPayment(); return new com.payflux.order.OrderDtos.PaymentSummaryDto(p.getId(), p.getOrder().getId(), p.getAmount(), p.getCurrency(), p.getMethod(), p.getMethodSummary(), p.getStatus(), a.getRiskLevel(), a.getRiskScore(), p.getCustomerEmail(), p.getRefundedAmount(), p.getCreatedAt()); }
}
