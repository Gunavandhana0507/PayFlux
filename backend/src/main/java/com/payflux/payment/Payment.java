package com.payflux.payment;

import com.payflux.fraud.FraudAnalysis;
import com.payflux.merchant.Merchant;
import com.payflux.order.Order;
import com.payflux.processor.ProcessorOutcome;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_payment_order_idempotency",
                        columnNames = {"order_id", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private FraudAnalysis fraudAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "processor_ref")
    private String processorRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "method_summary")
    private String methodSummary;

    @Column(name = "refunded_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulated_outcome")
    private ProcessorOutcome simulatedOutcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = PaymentStatus.CREATED;
        if (refundedAmount == null) refundedAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
