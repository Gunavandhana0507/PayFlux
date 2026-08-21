package com.payflux.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "payment",
        indexes = {@Index(name = "idx_payment_order", columnList = "order_id")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_payment_order_idempotency",
                    columnNames = {"order_id", "idempotency_key"})
        })
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.CREATED;

    /** Client-supplied X-Idempotency-Key (SRS REQ-PAY-6), unique per order. */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(length = 64)
    private String processorReference;

    @Column(length = 512)
    private String processorMessage;

    @Column(length = 64)
    private String failureCode;

    @Column(length = 512)
    private String failureReason;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(length = 4)
    private String cardLast4;

    @Column(length = 64)
    private String cardNetwork;

    @Column(length = 128)
    private String upiVpa;

    @Column(length = 64)
    private String bankCode;

    @Column(length = 64)
    private String walletProvider;

    @Column(length = 128)
    private String deviceFingerprint;

    @Column(length = 64)
    private String ipAddress;

    /** Mock-processor outcome requested at checkout, replayed after step-up verification. */
    @Column(length = 16)
    private String simulateOutcome;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant capturedAt;

    public BigDecimal refundableAmount() {
        return amount.subtract(refundedAmount);
    }
}
