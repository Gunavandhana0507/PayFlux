package com.payflux.payment;

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
    REFUNDED
}
