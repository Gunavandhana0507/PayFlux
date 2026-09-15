package com.payflux.merchant;

import com.payflux.auth.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "merchant")
@Getter @Setter @NoArgsConstructor
public class Merchant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true) private AppUser user;
    @Column(name = "business_name", nullable = false) private String businessName;
    @Column(name = "business_type", nullable = false) private String businessType;
    @Column(name = "gst_id", nullable = false) private String gstId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MerchantStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (status == null) status = MerchantStatus.ACTIVE; }
}
