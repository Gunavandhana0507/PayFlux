package com.payflux.api.repo;

import com.payflux.api.domain.Refund;
import com.payflux.api.domain.RefundStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, String> {

    List<Refund> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    Page<Refund> findByPaymentOrderMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    List<Refund> findByStatusOrderByCreatedAtAsc(RefundStatus status);

    List<Refund> findByPaymentIdAndStatusIn(String paymentId, Collection<RefundStatus> statuses);
}
