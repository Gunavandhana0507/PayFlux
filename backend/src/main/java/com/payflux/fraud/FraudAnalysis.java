package com.payflux.fraud;

import com.payflux.payment.Payment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_analysis")
@Getter @Setter @NoArgsConstructor
public class FraudAnalysis {
    @Id private String id;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false, unique = true) private Payment payment;
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 4) private BigDecimal riskScore;
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false) private RiskLevel riskLevel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Prediction prediction;
    @Column(name = "model_version", nullable = false) private String modelVersion;
    @Enumerated(EnumType.STRING) @Column(name = "analysis_status", nullable = false) private AnalysisStatus analysisStatus;
    @Lob @Column(name = "features_json", nullable = false, columnDefinition = "TEXT") private String featuresJson;
    @Lob @Column(name = "factors_json", nullable = false, columnDefinition = "TEXT") private String factorsJson;
    @Enumerated(EnumType.STRING) @Column(name = "merchant_feedback") private MerchantFeedback merchantFeedback;
    @Column(name = "feedback_at") private Instant feedbackAt;
    @Column(name = "feedback_by_user_id") private Long feedbackByUserId;
    @Column(name = "feedback_note", length = 500) private String feedbackNote;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
