package com.payflux.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payment_transition_log")
@Getter @Setter @NoArgsConstructor
public class PaymentTransitionLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", nullable = false) private PaymentStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false) private PaymentStatus toStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransitionActor actor;
    @Column(nullable = false) private String reason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
