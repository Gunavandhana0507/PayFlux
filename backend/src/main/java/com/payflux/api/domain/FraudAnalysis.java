package com.payflux.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    /** ALLOW | VERIFY | REJECT - what the gateway did with this score (SRS 4.4). */
    @Column(nullable = false, length = 32)
    private String decision;

    /** Probabilistic label the model reports, never a certain fraud determination (REQ-FRD-8). */
    @Column(nullable = false, length = 32)
    private String prediction;

    @Column(nullable = false, length = 32)
    private String analysisStatus = "COMPLETED";

    @Column(nullable = false, length = 32)
    private String modelVersion;

    /** JSON array of {code, description, weight} - the contributing risk factors. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String factorsJson;

    /**
     * The feature values the score was computed from. Persisted as rows so merchant
     * feedback can later be joined to the original features for retraining.
     */
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<FraudFeature> features = new ArrayList<>();

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
