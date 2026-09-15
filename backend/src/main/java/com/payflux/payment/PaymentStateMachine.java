package com.payflux.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Component
public class PaymentStateMachine {
    private final PaymentTransitionLogRepository logRepository;
    private final Map<PaymentStatus, EnumSet<PaymentStatus>> allowed = new EnumMap<>(PaymentStatus.class);
    public PaymentStateMachine(PaymentTransitionLogRepository logRepository) {
        this.logRepository = logRepository;
        allowed.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.INITIATED));
        allowed.put(PaymentStatus.INITIATED, EnumSet.of(PaymentStatus.FRAUD_CHECK));
        allowed.put(PaymentStatus.FRAUD_CHECK, EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.VERIFICATION_REQUIRED, PaymentStatus.REJECTED));
        allowed.put(PaymentStatus.VERIFICATION_REQUIRED, EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.REJECTED));
        allowed.put(PaymentStatus.AUTHORIZED, EnumSet.of(PaymentStatus.PROCESSING));
        allowed.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED));
        allowed.put(PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED));
        allowed.put(PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED));
    }
    @Transactional
    public void transition(Payment payment, PaymentStatus to, TransitionActor actor, String reason) {
        PaymentStatus from = payment.getStatus();
        if (!allowed.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class)).contains(to)) throw new IllegalStateException("Invalid payment transition");
        payment.setStatus(to);
        PaymentTransitionLog log = new PaymentTransitionLog(); log.setPayment(payment); log.setFromStatus(from); log.setToStatus(to); log.setActor(actor); log.setReason(reason); logRepository.save(log);
    }
}
