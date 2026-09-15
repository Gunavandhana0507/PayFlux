package com.payflux.payment;

import com.payflux.order.OrderDtos;

import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public OrderDtos.PaymentSummaryDto toSummary(Payment payment) {
        var analysis = payment.getFraudAnalysis();
        return new OrderDtos.PaymentSummaryDto(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getMethodSummary(),
                payment.getStatus(),
                analysis == null ? null : analysis.getRiskLevel(),
                analysis == null ? null : analysis.getRiskScore(),
                payment.getCustomerEmail(),
                payment.getRefundedAmount(),
                payment.getCreatedAt());
    }

    public OrderDtos.PublicPaymentDto toPublic(Payment payment) {
        return new OrderDtos.PublicPaymentDto(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getMethodSummary(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getFailureReason(),
                payment.getStatus() == PaymentStatus.VERIFICATION_REQUIRED,
                payment.getCreatedAt());
    }
}
