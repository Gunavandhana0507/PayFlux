package com.payflux.api.service;

import com.payflux.api.config.PayFluxProperties;
import com.payflux.api.domain.Merchant;
import com.payflux.api.domain.OrderEntity;
import com.payflux.api.domain.OrderStatus;
import com.payflux.api.dto.CreateOrderRequest;
import com.payflux.api.dto.OrderResponse;
import com.payflux.api.dto.PublicOrderResponse;
import com.payflux.api.repo.OrderRepository;
import com.payflux.api.web.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PayFluxProperties properties;

    public OrderService(OrderRepository orderRepository, PayFluxProperties properties) {
        this.orderRepository = orderRepository;
        this.properties = properties;
    }

    @Transactional
    public OrderResponse create(Merchant merchant, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        OrderEntity order = new OrderEntity();
        order.setMerchant(merchant);
        order.setAmount(request.amount());
        order.setCurrency(request.currency() == null ? "INR" : request.currency().toUpperCase());
        order.setReceipt(request.receipt());
        order.setDescription(request.description());
        order.setNotes(request.notes());
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerPhone(request.customerPhone());
        order.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        long expiryMinutes = request.expiryMinutes() == null || request.expiryMinutes() <= 0
                ? properties.getOrder().getExpiryMinutes()
                : request.expiryMinutes();
        order.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderEntity> listForMerchant(
            String merchantId, OrderStatus status, Instant from, Instant to, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (status == null && from == null && to == null) {
            return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
        }
        return orderRepository.search(
                merchantId, status, from == null ? Instant.EPOCH : from, to == null ? Instant.now() : to, pageable);
    }

    @Transactional
    public OrderEntity getForMerchant(String merchantId, String orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw ApiException.notFound("Order not found");
        }
        return expireIfDue(order);
    }

    @Transactional
    public OrderEntity getPublic(String orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        return expireIfDue(order);
    }

    /** Maps inside the transaction so the lazy merchant association is initialized. */
    @Transactional
    public PublicOrderResponse getPublicResponse(String orderId) {
        return PublicOrderResponse.from(getPublic(orderId));
    }

    public OrderResponse toResponse(OrderEntity order) {
        return OrderResponse.from(order, properties.getCheckoutBaseUrl());
    }

    /** Lazily expires an unpaid order once its 15-minute window has elapsed. */
    private OrderEntity expireIfDue(OrderEntity order) {
        boolean open = order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.ATTEMPTED;
        if (open && order.isExpired()) {
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        }
        return order;
    }

    /** Safety net for orders nobody opens again after they expire. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireStaleOrders() {
        List<OrderEntity> stale = orderRepository.findByStatusInAndExpiresAtBefore(
                List.of(OrderStatus.CREATED, OrderStatus.ATTEMPTED), Instant.now());
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(order -> {
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(Instant.now());
        });
        orderRepository.saveAll(stale);
        log.info("Expired {} stale order(s)", stale.size());
    }
}
