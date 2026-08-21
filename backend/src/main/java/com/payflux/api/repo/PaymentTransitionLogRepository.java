package com.payflux.api.repo;

import com.payflux.api.domain.PaymentTransitionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransitionLogRepository extends JpaRepository<PaymentTransitionLog, String> {

    List<PaymentTransitionLog> findByPaymentIdOrderByCreatedAtAsc(String paymentId);
}
