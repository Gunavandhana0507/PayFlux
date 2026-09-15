package com.payflux.order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);
    Optional<Order> findByIdAndMerchantId(String id, Long merchantId);
    Page<Order> findByMerchantId(Long merchantId, Pageable pageable);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, Instant time);
    @Modifying @Query("update Order o set o.status = 'EXPIRED' where o.status = 'CREATED' and o.expiresAt < :now") int expire(@Param("now") Instant now);
}
