package com.payflux.api.domain;

import java.util.Set;

/**
 * Payment state machine. Allowed transitions are declared on each state, and
 * {@link #canTransitionTo(PaymentStatus)} is the single source of truth used by
 * the payment service before any status change is persisted.
 */
public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    REQUIRES_VERIFICATION,
    CAPTURED,
    FAILED,
    TIMED_OUT,
    CANCELLED,
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
            case INITIATED -> Set.of(PROCESSING, FAILED, CANCELLED, TIMED_OUT);
            case PROCESSING -> Set.of(REQUIRES_VERIFICATION, CAPTURED, FAILED, TIMED_OUT);
            case REQUIRES_VERIFICATION -> Set.of(PROCESSING, FAILED, CANCELLED, TIMED_OUT);
            case CAPTURED -> Set.of(PARTIALLY_REFUNDED, REFUNDED);
            case PARTIALLY_REFUNDED -> Set.of(PARTIALLY_REFUNDED, REFUNDED);
            case FAILED, TIMED_OUT, CANCELLED, REFUNDED -> Set.of();
        };
    }
}
