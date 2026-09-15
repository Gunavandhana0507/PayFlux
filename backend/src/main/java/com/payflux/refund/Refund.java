package com.payflux.refund;

import com.payflux.merchant.Merchant;
import com.payflux.payment.Payment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refund")
@Getter @Setter @NoArgsConstructor
public class Refund {
    @Id private String id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "merchant_id", nullable = false) private Merchant merchant;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RefundStatus status;
    @Column(length = 500) private String reason;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "processed_at") private Instant processedAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (status == null) status = RefundStatus.PENDING; }
}
