package com.payflux.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
public class OrderExpiryJob {
    private final OrderRepository orderRepository;
    public OrderExpiryJob(OrderRepository orderRepository) { this.orderRepository = orderRepository; }
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expire() { orderRepository.expire(Instant.now()); }
}
