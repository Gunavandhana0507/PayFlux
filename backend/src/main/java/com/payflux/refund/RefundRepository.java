package com.payflux.refund;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.*;

public interface RefundRepository
        extends JpaRepository<Refund, String>, JpaSpecificationExecutor<Refund> {
    Page<Refund> findByMerchantId(Long merchantId, Pageable pageable);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByMerchantIdAndStatus(Long merchantId, RefundStatus status);

    List<Refund> findByPaymentId(String paymentId);
}
