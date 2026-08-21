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
@Table(name = "fraud_analysis")
@Getter
@Setter
public class FraudAnalysis {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel riskLevel;

    /** ALLOW | STEP_UP | REVIEW - what the gateway did with this score. */
    @Column(nullable = false, length = 32)
    private String decision;

    @Column(nullable = false, length = 32)
    private String modelVersion;

    /** JSON array of {code, description, weight} - the contributing risk factors. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String factorsJson;

    /**
     * JSON object with the feature values the score was computed from. Persisted so
     * merchant feedback can later be joined to the original features for retraining.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String featuresJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MerchantFeedback merchantFeedback;

    @Column(length = 512)
    private String feedbackNote;

    @Column(length = 190)
    private String feedbackByEmail;

    private Instant feedbackAt;
}
