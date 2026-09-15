package com.payflux.payment;
import com.payflux.fraud.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderIdAndIdempotencyKey(String orderId, String key);
    Optional<Payment> findByIdAndMerchantId(String id, Long merchantId);
    List<Payment> findByOrderIdOrderByCreatedAtDesc(String orderId);
    Page<Payment> findByMerchantId(Long merchantId, Pageable pageable);
    List<Payment> findByMerchantIdAndCustomerEmailAndCreatedAtAfter(Long merchantId, String email, Instant after);
    List<Payment> findByMerchantIdAndCreatedAtBetween(Long merchantId, Instant from, Instant to);
    List<Payment> findByMerchantIdAndStatusIn(Long merchantId, Collection<PaymentStatus> statuses);
    List<Payment> findByMerchantIdAndCreatedAtAfterAndStatusIn(Long merchantId, Instant after, Collection<PaymentStatus> statuses);
    long countByMerchantIdAndStatusIn(Long merchantId, Collection<PaymentStatus> statuses);
    @Query("select p from Payment p join fetch p.order o where p.merchant.id = :merchantId and p.createdAt >= :after and p.status in :statuses order by p.createdAt desc") List<Payment> findRecentByStatuses(@Param("merchantId") Long merchantId, @Param("after") Instant after, @Param("statuses") Collection<PaymentStatus> statuses);
}
