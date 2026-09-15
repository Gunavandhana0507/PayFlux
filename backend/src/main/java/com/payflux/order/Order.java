package com.payflux.order;

import com.payflux.merchant.Merchant;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "orders",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_order_merchant_idempotency",
                        columnNames = {"merchant_id", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 500)
    private String notes;

    @Column(name = "customer_email")
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = OrderStatus.CREATED;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
