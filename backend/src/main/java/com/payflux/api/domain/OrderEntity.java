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
        name = "`order`",
        indexes = {@Index(name = "idx_order_merchant", columnList = "merchant_id")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_order_merchant_idempotency",
                    columnNames = {"merchant_id", "idempotency_key"})
        })
@Getter
@Setter
public class OrderEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 64)
    private String receipt;

    @Column(length = 512)
    private String description;

    /** Free-form merchant notes carried on the order (SRS REQ-ORD-1). */
    @Column(length = 1024)
    private String notes;

    private String customerName;

    @Column(length = 190)
    private String customerEmail;

    @Column(length = 32)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant paidAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isPayable() {
        return (status == OrderStatus.CREATED || status == OrderStatus.ATTEMPTED) && !isExpired();
    }
}
