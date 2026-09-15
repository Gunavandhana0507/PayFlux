package com.payflux.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentTransitionLogRepository extends JpaRepository<PaymentTransitionLog, Long> { List<PaymentTransitionLog> findByPaymentIdOrderByCreatedAtAsc(String paymentId); }
