package com.payflux.refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RefundRepository extends JpaRepository<Refund, String> { Page<Refund> findByMerchantId(Long merchantId, Pageable pageable); List<Refund> findByStatus(RefundStatus status); List<Refund> findByMerchantIdAndStatus(Long merchantId, RefundStatus status); List<Refund> findByPaymentId(String paymentId); }
