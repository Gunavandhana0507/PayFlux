package com.payflux.api.repo;

import com.payflux.api.domain.FraudAnalysis;
import com.payflux.api.domain.RiskLevel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, String> {

    Optional<FraudAnalysis> findByPaymentId(String paymentId);

    List<FraudAnalysis> findByPaymentIdIn(Collection<String> paymentIds);

    Optional<FraudAnalysis> findByIdAndPaymentOrderMerchantId(String id, String merchantId);

    Page<FraudAnalysis> findByPaymentOrderMerchantIdAndRiskLevelInOrderByCreatedAtDesc(
            String merchantId, Collection<RiskLevel> riskLevels, Pageable pageable);
}
