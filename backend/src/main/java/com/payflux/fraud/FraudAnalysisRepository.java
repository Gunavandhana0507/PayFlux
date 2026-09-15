package com.payflux.fraud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, String> { Optional<FraudAnalysis> findByPaymentId(String paymentId); Page<FraudAnalysis> findByPaymentMerchantIdAndRiskLevelIn(Long merchantId, Collection<RiskLevel> levels, Pageable pageable); List<FraudAnalysis> findTop5ByPaymentMerchantIdAndRiskLevelInOrderByCreatedAtDesc(Long merchantId, Collection<RiskLevel> levels); }
