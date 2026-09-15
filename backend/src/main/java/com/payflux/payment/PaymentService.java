package com.payflux.payment;

import com.payflux.common.ApiExceptions.BusinessRuleException;
import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.PageResponse;
import com.payflux.fraud.FraudAnalysis;
import com.payflux.fraud.FraudAnalysisRepository;
import com.payflux.fraud.FraudService;
import com.payflux.merchant.Merchant;
import com.payflux.order.Order;
import com.payflux.order.OrderDtos;
import com.payflux.order.OrderService;
import com.payflux.order.OrderStatus;
import com.payflux.processor.MockPaymentProcessor;
import com.payflux.processor.ProcessorOutcome;
import com.payflux.refund.RefundService;
import com.payflux.security.AuthFacade;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final OrderService orderService;
    private final PaymentStateMachine stateMachine;
    private final FraudService fraudService;
    private final MockPaymentProcessor processor;
    private final AuthFacade authFacade;
    private final RefundService refundService;
    private final PaymentTransitionLogRepository transitionLogRepository;
    public PaymentService(PaymentRepository paymentRepository, FraudAnalysisRepository fraudAnalysisRepository, OrderService orderService, PaymentStateMachine stateMachine, FraudService fraudService, MockPaymentProcessor processor, AuthFacade authFacade, RefundService refundService, PaymentTransitionLogRepository transitionLogRepository) { this.paymentRepository = paymentRepository; this.fraudAnalysisRepository = fraudAnalysisRepository; this.orderService = orderService; this.stateMachine = stateMachine; this.fraudService = fraudService; this.processor = processor; this.authFacade = authFacade; this.refundService = refundService; this.transitionLogRepository = transitionLogRepository; }
    @Transactional
    public OrderDtos.PublicPaymentDto createPublic(String orderId, String key, PaymentDtos.PaymentRequest request) {
        if (key != null && !key.isBlank()) { var existing = paymentRepository.findByOrderIdAndIdempotencyKey(orderId, key); if (existing.isPresent()) return publicDto(existing.get()); }
        Order order = orderService.getForPayment(orderId);
        if (order.getStatus() == OrderStatus.EXPIRED || order.getExpiresAt().isBefore(java.time.Instant.now())) throw new BusinessRuleException("ORDER_EXPIRED", "This payment link has expired. Ask the merchant for a new one.");
        if (order.getStatus() == OrderStatus.PAID) throw new BusinessRuleException("ORDER_ALREADY_PAID", "This order has already been paid");
        validate(request);
        Payment p = new Payment(); p.setId(com.payflux.common.IdGenerator.next("pay_")); p.setOrder(order); p.setMerchant(order.getMerchant()); p.setMethod(request.method()); p.setAmount(order.getAmount()); p.setCurrency(order.getCurrency()); p.setCustomerEmail(request.customerEmail()); p.setDeviceId(request.deviceId()); p.setMethodSummary(summary(request)); p.setIdempotencyKey(key); p.setSimulatedOutcome(request.simulateOutcome()); paymentRepository.save(p);
        stateMachine.transition(p, PaymentStatus.INITIATED, TransitionActor.CUSTOMER, "Payment initiated"); stateMachine.transition(p, PaymentStatus.FRAUD_CHECK, TransitionActor.SYSTEM, "Fraud check started");
        FraudService.FraudAssessment assessment = fraudService.assess(p);
        if (assessment.analysis().getRiskLevel() == com.payflux.fraud.RiskLevel.HIGH) { stateMachine.transition(p, PaymentStatus.REJECTED, TransitionActor.SYSTEM, "Flagged as high risk"); p.setFailureReason("We couldn't complete this payment. Please contact the merchant."); }
        else if (assessment.analysis().getRiskLevel() == com.payflux.fraud.RiskLevel.MEDIUM) stateMachine.transition(p, PaymentStatus.VERIFICATION_REQUIRED, TransitionActor.CUSTOMER, "Additional verification required");
        else { stateMachine.transition(p, PaymentStatus.AUTHORIZED, TransitionActor.SYSTEM, "Low risk payment authorized"); continueToCapture(p); }
        return publicDto(p);
    }
    @Transactional
    public OrderDtos.PublicPaymentDto verify(String id, PaymentDtos.VerifyRequest request) {
        Payment p = find(id);
        if (p.getStatus() != PaymentStatus.VERIFICATION_REQUIRED) throw new BusinessRuleException("VERIFICATION_NOT_ALLOWED", "This payment does not require verification");
        if (!"123456".equals(request.otp())) { stateMachine.transition(p, PaymentStatus.REJECTED, TransitionActor.CUSTOMER, "Verification failed"); p.setFailureReason("The verification code was incorrect. This payment was cancelled."); return publicDto(p); }
        stateMachine.transition(p, PaymentStatus.AUTHORIZED, TransitionActor.CUSTOMER, "Verification succeeded"); continueToCapture(p); return publicDto(p);
    }
    private void continueToCapture(Payment p) {
        stateMachine.transition(p, PaymentStatus.PROCESSING, TransitionActor.SYSTEM, "Payment sent to processor");
        var result = processor.process(p, p.getSimulatedOutcome());
        if (result.outcome() == ProcessorOutcome.SUCCESS) { p.setProcessorRef(result.processorRef()); stateMachine.transition(p, PaymentStatus.CAPTURED, TransitionActor.SYSTEM, "Payment captured"); p.getOrder().setStatus(OrderStatus.PAID); }
        else { stateMachine.transition(p, PaymentStatus.FAILED, TransitionActor.SYSTEM, "Processor declined payment"); p.setFailureReason(result.message()); }
    }
    @Transactional(readOnly = true) public OrderDtos.PublicPaymentDto publicPayment(String id) { return publicDto(find(id)); }
    @Transactional(readOnly = true) public PageResponse<OrderDtos.PaymentSummaryDto> list(int page, int size) { Merchant m = authFacade.currentMerchant(); var result = paymentRepository.findByMerchantId(m.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))); return new PageResponse<>(result.getContent().stream().map(this::summary).toList(), page, size, result.getTotalElements()); }
    @Transactional(readOnly = true) public Payment findOwned(String id) { return paymentRepository.findByIdAndMerchantId(id, authFacade.currentMerchant().getId()).orElseThrow(() -> new NotFoundException("Payment was not found")); }
    @Transactional(readOnly = true) public PaymentDtos.PaymentDetailDto detail(String id) {
        Payment p = findOwned(id);
        FraudAnalysis analysis = fraudAnalysisRepository.findByPaymentId(id).orElse(null);
        BigDecimal refundable = p.getAmount().subtract(p.getRefundedAmount()).subtract(refundService.pendingAmount(p.getId()));
        var transitions = transitionLogRepository.findByPaymentIdOrderByCreatedAtAsc(id).stream().map(t -> new PaymentDtos.TransitionDto(t.getFromStatus(), t.getToStatus(), t.getActor(), t.getReason(), t.getCreatedAt())).toList();
        return new PaymentDtos.PaymentDetailDto(summary(p), p.getFailureReason(), p.getDeviceId(), p.getProcessorRef(), orderService.toDto(p.getOrder()), analysis == null ? null : fraudService.toDto(analysis), refundService.listForPayment(id), transitions, refundable);
    }
    private Payment find(String id) { return paymentRepository.findById(id).orElseThrow(() -> new NotFoundException("Payment was not found")); }
    private OrderDtos.PublicPaymentDto publicDto(Payment p) { return new OrderDtos.PublicPaymentDto(p.getId(), p.getOrder().getId(), p.getStatus(), p.getMethod(), p.getMethodSummary(), p.getAmount(), p.getCurrency(), p.getFailureReason(), p.getStatus() == PaymentStatus.VERIFICATION_REQUIRED, p.getCreatedAt()); }
    private OrderDtos.PaymentSummaryDto summary(Payment p) { var a = fraudAnalysisRepository.findByPaymentId(p.getId()).orElse(null); return new OrderDtos.PaymentSummaryDto(p.getId(), p.getOrder().getId(), p.getAmount(), p.getCurrency(), p.getMethod(), p.getMethodSummary(), p.getStatus(), a == null ? null : a.getRiskLevel(), a == null ? null : a.getRiskScore(), p.getCustomerEmail(), p.getRefundedAmount(), p.getCreatedAt()); }
    private String summary(PaymentDtos.PaymentRequest r) { return switch (r.method()) { case CARD -> "Card •••• " + r.card().number().substring(r.card().number().length() - 4); case UPI -> "UPI " + r.upiId(); case NETBANKING -> "Netbanking " + r.bankCode(); case WALLET -> "Wallet " + r.walletProvider(); }; }
    private void validate(PaymentDtos.PaymentRequest r) {
        if (r.method() == PaymentMethod.CARD && (r.card() == null || r.card().number() == null || !r.card().number().matches("[0-9]{13,19}"))) {
            throw new BusinessRuleException("INVALID_PAYMENT_DETAILS", "Enter a valid card number");
        }
        if (r.method() == PaymentMethod.UPI && (r.upiId() == null || !r.upiId().matches("^[A-Za-z0-9_.-]+@[A-Za-z0-9_]+$"))) {
            throw new BusinessRuleException("INVALID_PAYMENT_DETAILS", "Enter a valid UPI ID");
        }
        if (r.method() == PaymentMethod.NETBANKING && (r.bankCode() == null || r.bankCode().isBlank())) {
            throw new BusinessRuleException("INVALID_PAYMENT_DETAILS", "Enter a bank code");
        }
        if (r.method() == PaymentMethod.WALLET && (r.walletProvider() == null || r.walletProvider().isBlank())) {
            throw new BusinessRuleException("INVALID_PAYMENT_DETAILS", "Enter a wallet provider");
        }
    }
}
