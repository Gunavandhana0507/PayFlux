package com.payflux.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "merchant")
@Getter
@Setter
public class Merchant {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private String businessName;

    private String legalName;

    @Column(length = 64)
    private String businessType;

    private String websiteUrl;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false, length = 190)
    private String contactEmail;

    @Column(nullable = false, length = 32)
    private String contactPhone;

    @Column(length = 32)
    private String panNumber;

    @Column(length = 32)
    private String gstin;

    private String addressLine1;

    private String addressLine2;

    @Column(length = 96)
    private String city;

    @Column(length = 96)
    private String state;

    @Column(length = 16)
    private String postalCode;

    @Column(length = 64)
    private String country;

    private String bankAccountName;

    @Column(length = 34)
    private String bankAccountNumber;

    @Column(length = 16)
    private String bankIfsc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
