package com.payflux.refund;

import com.payflux.common.ApiExceptions.BusinessRuleException;
import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.IdGenerator;
import com.payflux.common.PageResponse;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStateMachine;
import com.payflux.payment.PaymentStatus;
import com.payflux.payment.TransitionActor;
import com.payflux.security.AuthFacade;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final AuthFacade authFacade;

    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository,
            PaymentStateMachine stateMachine,
            AuthFacade authFacade) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.authFacade = authFacade;
    }

    @Transactional
    public RefundDtos.RefundDto create(String paymentId, RefundDtos.CreateRefundRequest request) {
        Payment p =
                paymentRepository
                        .findByIdAndMerchantId(paymentId, authFacade.currentMerchant().getId())
                        .orElseThrow(() -> new NotFoundException("Payment was not found"));
        if (p.getStatus() != PaymentStatus.CAPTURED
                && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw new BusinessRuleException(
                    "REFUND_NOT_ALLOWED", "Only successful payments can be refunded");
        BigDecimal remaining =
                p.getAmount().subtract(p.getRefundedAmount()).subtract(pendingAmount(paymentId));
        if (request.amount().compareTo(remaining) > 0)
            throw new BusinessRuleException(
                    "REFUND_EXCEEDS_BALANCE",
                    "Refund amount is more than the remaining ₹" + remaining.setScale(2));
        Refund r = new Refund();
        r.setId(IdGenerator.next("rfnd_"));
        r.setPayment(p);
        r.setMerchant(p.getMerchant());
        r.setAmount(request.amount());
        r.setReason(request.reason());
        r.setStatus(RefundStatus.PENDING);
        r.setCreatedAt(Instant.now());
        return toDto(refundRepository.save(r));
    }

    @Transactional(readOnly = true)
    public BigDecimal pendingAmount(String paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream()
                .filter(
                        r ->
                                r.getStatus() == RefundStatus.PENDING
                                        || r.getStatus() == RefundStatus.PROCESSING)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public java.util.List<RefundDtos.RefundDto> listForPayment(String paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundDtos.RefundDto> list(int page, int size, RefundStatus status) {
        Long merchantId = authFacade.currentMerchant().getId();
        Specification<Refund> specification =
                (root, query, criteriaBuilder) -> {
                    var predicates = new ArrayList<Predicate>();
                    predicates.add(
                            criteriaBuilder.equal(root.get("merchant").get("id"), merchantId));
                    if (status != null) {
                        predicates.add(criteriaBuilder.equal(root.get("status"), status));
                    }
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
        var result =
                refundRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RefundDtos.RefundDto detail(String id) {
        Refund r =
                refundRepository
                        .findById(id)
                        .filter(
                                x ->
                                        x.getMerchant()
                                                .getId()
                                                .equals(authFacade.currentMerchant().getId()))
                        .orElseThrow(() -> new NotFoundException("Refund was not found"));
        return toDto(r);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPending() {
        for (Refund r : refundRepository.findByStatus(RefundStatus.PENDING)) {
            r.setStatus(RefundStatus.PROCESSING);
            if (r.getReason() != null && r.getReason().toLowerCase().contains("simulate-fail")) {
                r.setStatus(RefundStatus.FAILED);
                r.setFailureReason("The bank could not process this refund");
            } else {
                r.setStatus(RefundStatus.PROCESSED);
                r.setProcessedAt(Instant.now());
                Payment p = r.getPayment();
                p.setRefundedAmount(p.getRefundedAmount().add(r.getAmount()));
                PaymentStatus next =
                        p.getRefundedAmount().compareTo(p.getAmount()) == 0
                                ? PaymentStatus.REFUNDED
                                : PaymentStatus.PARTIALLY_REFUNDED;
                stateMachine.transition(p, next, TransitionActor.SYSTEM, "Refund processed");
            }
            refundRepository.save(r);
        }
    }

    public RefundDtos.RefundDto toDto(Refund r) {
        return new RefundDtos.RefundDto(
                r.getId(),
                r.getPayment().getId(),
                r.getPayment().getOrder().getId(),
                r.getAmount(),
                r.getPayment().getCurrency(),
                r.getStatus(),
                r.getReason(),
                r.getFailureReason(),
                r.getCreatedAt(),
                r.getProcessedAt());
    }
}
