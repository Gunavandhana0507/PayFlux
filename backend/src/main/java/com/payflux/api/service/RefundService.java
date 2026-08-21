package com.payflux.api.service;

import com.payflux.api.domain.Payment;
import com.payflux.api.domain.PaymentStatus;
import com.payflux.api.domain.Refund;
import com.payflux.api.domain.RefundStatus;
import com.payflux.api.dto.RefundRequest;
import com.payflux.api.dto.RefundResponse;
import com.payflux.api.repo.PaymentRepository;
import com.payflux.api.repo.RefundRepository;
import com.payflux.api.web.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MockPaymentProcessor processor;
    private final PaymentService paymentService;

    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository,
            MockPaymentProcessor processor,
            PaymentService paymentService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.processor = processor;
        this.paymentService = paymentService;
    }

    @Transactional
    public RefundResponse create(String merchantId, String paymentId, RefundRequest request, String initiatedBy) {
        Payment payment = paymentRepository
                .findByIdAndOrderMerchantId(paymentId, merchantId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw ApiException.badRequest("Only captured payments can be refunded");
        }

        BigDecimal refundable = payment.refundableAmount();
        BigDecimal amount = request.amount() == null ? refundable : request.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Refund amount must be greater than zero");
        }
        if (amount.compareTo(refundable) > 0) {
            throw ApiException.badRequest(
                    "Refund amount exceeds the refundable balance of " + refundable.toPlainString());
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(amount);
        refund.setReason(request.reason());
        refund.setInitiatedBy(initiatedBy);
        refund.setProcessorReference(processor.refund(payment));
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedAt(Instant.now());
        refundRepository.save(refund);

        BigDecimal refundedTotal = payment.getRefundedAmount().add(amount);
        payment.setRefundedAmount(refundedTotal);
        PaymentStatus target = refundedTotal.compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        paymentService.transition(
                payment, target, "Refund of %s processed".formatted(amount.toPlainString()), "MERCHANT");

        return RefundResponse.from(refund);
    }

    @Transactional(readOnly = true)
    public Page<Refund> listForMerchant(String merchantId, int page, int size) {
        return refundRepository.findByPaymentOrderMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size));
    }
}
