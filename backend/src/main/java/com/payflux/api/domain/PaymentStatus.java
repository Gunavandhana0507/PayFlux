package com.payflux.api.domain;

import java.util.Set;

/**
 * Payment State Machine from SRS section 4.3 (REQ-PAY-5):
 * CREATED -> INITIATED -> FRAUD_CHECK -> {AUTHORIZED | VERIFICATION_REQUIRED | REJECTED}
 * -> PROCESSING -> {CAPTURED | FAILED} -> {PARTIALLY_REFUNDED | REFUNDED}.
 * Any transition not declared here is rejected by the payment service.
 */
public enum PaymentStatus {
    CREATED,
    INITIATED,
    FRAUD_CHECK,
    AUTHORIZED,
    VERIFICATION_REQUIRED,
    REJECTED,
    PROCESSING,
    CAPTURED,
    FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        return allowedTargets().contains(target);
    }

    public boolean isTerminal() {
        return allowedTargets().isEmpty();
    }

    private Set<PaymentStatus> allowedTargets() {
        return switch (this) {
            case CREATED -> Set.of(INITIATED, FAILED);
            case INITIATED -> Set.of(FRAUD_CHECK, FAILED);
            case FRAUD_CHECK -> Set.of(AUTHORIZED, VERIFICATION_REQUIRED, REJECTED);
            case VERIFICATION_REQUIRED -> Set.of(AUTHORIZED, REJECTED);
            case AUTHORIZED -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(CAPTURED, FAILED);
            case CAPTURED -> Set.of(PARTIALLY_REFUNDED, REFUNDED);
            case PARTIALLY_REFUNDED -> Set.of(PARTIALLY_REFUNDED, REFUNDED);
            case REJECTED, FAILED, REFUNDED -> Set.of();
        };
    }
}
