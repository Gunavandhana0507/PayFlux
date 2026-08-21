package com.payflux.api.service;

import com.payflux.api.domain.FraudAnalysis;
import com.payflux.api.domain.OrderEntity;
import com.payflux.api.domain.OrderStatus;
import com.payflux.api.domain.Payment;
import com.payflux.api.domain.PaymentStatus;
import com.payflux.api.domain.PaymentTransitionLog;
import com.payflux.api.dto.FraudAnalysisResponse;
import com.payflux.api.dto.InitiatePaymentRequest;
import com.payflux.api.dto.PaymentDetailResponse;
import com.payflux.api.dto.PaymentResponse;
import com.payflux.api.dto.PublicPaymentResponse;
import com.payflux.api.dto.RefundResponse;
import com.payflux.api.dto.TransitionResponse;
import com.payflux.api.repo.FraudAnalysisRepository;
import com.payflux.api.repo.OrderRepository;
import com.payflux.api.repo.PaymentRepository;
import com.payflux.api.repo.PaymentTransitionLogRepository;
import com.payflux.api.repo.RefundRepository;
import com.payflux.api.service.MockPaymentProcessor.ProcessorResult;
import com.payflux.api.web.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    /** Fixed OTP accepted by the mock step-up challenge. */
    private static final String MOCK_OTP = "123456";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransitionLogRepository transitionLogRepository;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final RefundRepository refundRepository;
    private final MockPaymentProcessor processor;
    private final FraudService fraudService;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentTransitionLogRepository transitionLogRepository,
            FraudAnalysisRepository fraudAnalysisRepository,
            RefundRepository refundRepository,
            MockPaymentProcessor processor,
            FraudService fraudService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.transitionLogRepository = transitionLogRepository;
        this.fraudAnalysisRepository = fraudAnalysisRepository;
        this.refundRepository = refundRepository;
        this.processor = processor;
        this.fraudService = fraudService;
    }

    @Transactional
    public PublicPaymentResponse initiate(
            OrderEntity order, InitiatePaymentRequest request, String ipAddress, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByOrderIdAndIdempotencyKey(order.getId(), idempotencyKey);
            if (existing.isPresent()) {
                Payment payment = existing.get();
                return toPublicResponse(payment, nextActionFor(payment), payment.getProcessorMessage());
            }
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw ApiException.conflict("This order has already been paid");
        }
        if (!order.isPayable()) {
            throw ApiException.badRequest("This payment link has expired or is no longer payable");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.method());
        payment.setAmount(order.getAmount());
        payment.setDeviceFingerprint(request.deviceFingerprint());
        payment.setIpAddress(ipAddress);
        payment.setSimulateOutcome(request.simulateOutcome());
        payment.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        applyInstrument(payment, request);
        paymentRepository.save(payment);
        log(payment, null, PaymentStatus.CREATED, "Payment record created", "SYSTEM");
        transition(payment, PaymentStatus.INITIATED, "Payment initiated at checkout", "CUSTOMER");

        order.setStatus(OrderStatus.ATTEMPTED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        transition(payment, PaymentStatus.FRAUD_CHECK, "Running fraud risk assessment", "SYSTEM");
        FraudAnalysis analysis = fraudService.analyze(payment);

        // SRS 4.4 / business rules: High risk is rejected outright, Medium needs verification.
        switch (analysis.getDecision()) {
            case "REJECT" -> {
                payment.setFailureCode("FRAUD_REJECTED");
                payment.setFailureReason(
                        "Rejected by risk checks (risk score %d)".formatted(analysis.getRiskScore()));
                transition(payment, PaymentStatus.REJECTED, payment.getFailureReason(), "RISK_ENGINE");
                return toPublicResponse(payment, null, payment.getFailureReason());
            }
            case "VERIFY" -> {
                transition(
                        payment,
                        PaymentStatus.VERIFICATION_REQUIRED,
                        "Medium risk (score %d) - additional verification required".formatted(analysis.getRiskScore()),
                        "RISK_ENGINE");
                return toPublicResponse(
                        payment, "OTP_VERIFICATION", "Additional verification required. Use OTP " + MOCK_OTP + ".");
            }
            default -> transition(
                    payment, PaymentStatus.AUTHORIZED, "Low risk (score %d) - authorized".formatted(analysis.getRiskScore()), "RISK_ENGINE");
        }

        return authorizeWithProcessor(payment);
    }

    @Transactional
    public PublicPaymentResponse verify(String paymentId, String otp) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (payment.getStatus() != PaymentStatus.VERIFICATION_REQUIRED) {
            throw ApiException.badRequest("This payment is not awaiting verification");
        }
        if (!MOCK_OTP.equals(otp)) {
            payment.setAttemptCount(payment.getAttemptCount() + 1);
            paymentRepository.save(payment);
            if (payment.getAttemptCount() >= 3) {
                payment.setFailureCode("OTP_ATTEMPTS_EXCEEDED");
                payment.setFailureReason("Verification failed after 3 attempts");
                // REQ-FRD-5: failed verification results in rejection.
                transition(payment, PaymentStatus.REJECTED, payment.getFailureReason(), "SYSTEM");
                return toPublicResponse(payment, null, payment.getFailureReason());
            }
            throw ApiException.badRequest("Incorrect verification code");
        }
        transition(payment, PaymentStatus.AUTHORIZED, "Verification successful", "CUSTOMER");
        return authorizeWithProcessor(payment);
    }

    private PublicPaymentResponse authorizeWithProcessor(Payment payment) {
        transition(payment, PaymentStatus.PROCESSING, "Sent to payment processor", "SYSTEM");
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        ProcessorResult result = processor.authorize(payment, payment.getSimulateOutcome());
        payment.setProcessorReference(result.reference());
        payment.setProcessorMessage(result.message());

        switch (result.outcome()) {
            case SUCCESS -> {
                payment.setCapturedAt(Instant.now());
                transition(payment, PaymentStatus.CAPTURED, result.message(), "PROCESSOR");
                OrderEntity order = payment.getOrder();
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(Instant.now());
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);
                return toPublicResponse(payment, null, "Payment successful");
            }
            case FAILURE -> {
                payment.setFailureCode(result.failureCode());
                payment.setFailureReason(result.message());
                transition(payment, PaymentStatus.FAILED, result.message(), "PROCESSOR");
                return toPublicResponse(payment, null, result.message());
            }
            case TIMEOUT -> {
                // The state machine has no timeout state; a processor timeout terminates as FAILED.
                payment.setFailureCode(result.failureCode());
                payment.setFailureReason(result.message());
                transition(payment, PaymentStatus.FAILED, result.message(), "PROCESSOR");
                return toPublicResponse(payment, null, result.message());
            }
        }
        throw new IllegalStateException("Unhandled processor outcome");
    }

    /** Every status change goes through here so the state machine and the log stay in sync. */
    public void transition(Payment payment, PaymentStatus target, String reason, String actor) {
        PaymentStatus current = payment.getStatus();
        if (!current.canTransitionTo(target)) {
            throw ApiException.conflict("Illegal payment transition %s -> %s".formatted(current, target));
        }
        payment.setStatus(target);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        log(payment, current, target, reason, actor);
    }

    private void log(Payment payment, PaymentStatus from, PaymentStatus to, String reason, String actor) {
        PaymentTransitionLog entry = new PaymentTransitionLog();
        entry.setPayment(payment);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setReason(reason);
        entry.setActor(actor);
        transitionLogRepository.save(entry);
    }

    private void applyInstrument(Payment payment, InitiatePaymentRequest request) {
        switch (request.method()) {
            case CARD -> {
                String number = request.cardNumber() == null ? "" : request.cardNumber().replaceAll("\\s+", "");
                if (number.length() < 12) {
                    throw ApiException.badRequest("A valid card number is required");
                }
                payment.setCardLast4(number.substring(number.length() - 4));
                payment.setCardNetwork(detectNetwork(number));
            }
            case UPI -> {
                if (request.upiVpa() == null || !request.upiVpa().contains("@")) {
                    throw ApiException.badRequest("A valid UPI ID is required");
                }
                payment.setUpiVpa(request.upiVpa());
            }
            case NET_BANKING -> {
                if (request.bankCode() == null || request.bankCode().isBlank()) {
                    throw ApiException.badRequest("Select a bank to continue");
                }
                payment.setBankCode(request.bankCode());
            }
            case WALLET -> {
                if (request.walletProvider() == null || request.walletProvider().isBlank()) {
                    throw ApiException.badRequest("Select a wallet to continue");
                }
                payment.setWalletProvider(request.walletProvider());
            }
        }
    }

    private String detectNetwork(String number) {
        if (number.startsWith("4")) {
            return "VISA";
        }
        if (number.startsWith("5")) {
            return "MASTERCARD";
        }
        if (number.startsWith("6")) {
            return "RUPAY";
        }
        return "UNKNOWN";
    }

    @Transactional(readOnly = true)
    public PublicPaymentResponse getPublic(String paymentId) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        return toPublicResponse(payment, nextActionFor(payment), payment.getProcessorMessage());
    }

    @Transactional(readOnly = true)
    public Page<Payment> listForMerchant(String merchantId, PaymentStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return status == null
                ? paymentRepository.findByOrderMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                : paymentRepository.findByOrderMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> toResponses(List<Payment> payments) {
        Map<String, FraudAnalysis> analyses =
                fraudAnalysisRepository.findByPaymentIdIn(payments.stream().map(Payment::getId).toList()).stream()
                        .collect(Collectors.toMap(analysis -> analysis.getPayment().getId(), Function.identity()));
        return payments.stream()
                .map(payment -> toResponse(payment, analyses.get(payment.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getDetailForMerchant(String merchantId, String paymentId) {
        Payment payment = paymentRepository
                .findByIdAndOrderMerchantId(paymentId, merchantId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        FraudAnalysis analysis = fraudAnalysisRepository.findByPaymentId(paymentId).orElse(null);
        FraudAnalysisResponse analysisResponse = analysis == null ? null : fraudService.toResponse(analysis);
        List<TransitionResponse> transitions =
                transitionLogRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId).stream()
                        .map(TransitionResponse::from)
                        .toList();
        List<RefundResponse> refunds = refundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(RefundResponse::from)
                .toList();
        return new PaymentDetailResponse(toResponse(payment, analysis), analysisResponse, transitions, refunds);
    }

    @Transactional(readOnly = true)
    public List<Payment> listForOrder(String orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public PaymentResponse toResponse(Payment payment, FraudAnalysis analysis) {
        OrderEntity order = payment.getOrder();
        return new PaymentResponse(
                payment.getId(),
                order.getId(),
                order.getReceipt(),
                payment.getAmount(),
                order.getCurrency(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getRefundedAmount(),
                order.getCustomerEmail(),
                order.getCustomerName(),
                payment.getCardLast4(),
                payment.getUpiVpa(),
                payment.getBankCode(),
                payment.getWalletProvider(),
                payment.getProcessorReference(),
                payment.getFailureReason(),
                payment.getAttemptCount(),
                analysis == null ? null : analysis.getRiskScore(),
                analysis == null ? null : analysis.getRiskLevel().name(),
                analysis == null || analysis.getMerchantFeedback() == null
                        ? null
                        : analysis.getMerchantFeedback().name(),
                payment.getCreatedAt(),
                payment.getCapturedAt());
    }

    private String nextActionFor(Payment payment) {
        return payment.getStatus() == PaymentStatus.VERIFICATION_REQUIRED ? "OTP_VERIFICATION" : null;
    }

    private PublicPaymentResponse toPublicResponse(Payment payment, String nextAction, String message) {
        return new PublicPaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getStatus().name(),
                payment.getMethod().name(),
                payment.getAmount(),
                payment.getOrder().getCurrency(),
                nextAction,
                message,
                payment.getFailureReason(),
                payment.getCreatedAt());
    }
}
