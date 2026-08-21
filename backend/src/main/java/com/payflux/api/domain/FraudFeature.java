package com.payflux.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** One feature value used to produce a fraud analysis (ER entity fraud_feature). */
@Entity
@Table(
        name = "fraud_feature",
        indexes = {@Index(name = "idx_fraud_feature_analysis", columnList = "analysis_id")})
@Getter
@Setter
public class FraudFeature {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private FraudAnalysis analysis;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String value;

    @Column(name = "position", nullable = false)
    private int position;
}
