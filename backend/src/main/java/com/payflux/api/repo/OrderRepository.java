package com.payflux.api.repo;

import com.payflux.api.domain.OrderEntity;
import com.payflux.api.domain.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    Page<OrderEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    Optional<OrderEntity> findByMerchantIdAndIdempotencyKey(String merchantId, String idempotencyKey);

    List<OrderEntity> findByStatusInAndExpiresAtBefore(Collection<OrderStatus> statuses, Instant cutoff);

    long countByMerchantIdAndCustomerEmailIgnoreCase(String merchantId, String customerEmail);
}
