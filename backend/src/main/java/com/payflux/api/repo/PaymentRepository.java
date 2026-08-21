package com.payflux.api.repo;

import com.payflux.api.domain.Payment;
import com.payflux.api.domain.PaymentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(String orderId);

    Page<Payment> findByOrderMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    Page<Payment> findByOrderMerchantIdAndStatusOrderByCreatedAtDesc(
            String merchantId, PaymentStatus status, Pageable pageable);

    Optional<Payment> findByIdAndOrderMerchantId(String id, String merchantId);

    Optional<Payment> findByOrderIdAndIdempotencyKey(String orderId, String idempotencyKey);

    @Query(
            """
            select count(p) from Payment p
            where lower(p.order.customerEmail) = lower(:email)
              and p.status in :statuses
              and p.createdAt >= :since
            """)
    long countRecentByCustomerEmailAndStatuses(
            @Param("email") String email,
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("since") Instant since);

    @Query(
            """
            select count(p) from Payment p
            where lower(p.order.customerEmail) = lower(:email)
              and p.deviceFingerprint = :fingerprint
              and p.id <> :excludeId
            """)
    long countByCustomerEmailAndDevice(
            @Param("email") String email,
            @Param("fingerprint") String fingerprint,
            @Param("excludeId") String excludeId);
}
