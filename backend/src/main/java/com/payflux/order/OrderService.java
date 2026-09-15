package com.payflux.order;

import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.IdGenerator;
import com.payflux.common.PageResponse;
import com.payflux.merchant.Merchant;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentRepository;
import com.payflux.payment.PaymentStatus;
import com.payflux.security.AuthFacade;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AuthFacade authFacade;
    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository, AuthFacade authFacade) { this.orderRepository = orderRepository; this.paymentRepository = paymentRepository; this.authFacade = authFacade; }
    @Transactional
    public OrderDtos.OrderDto create(OrderDtos.CreateOrderRequest request, String key) {
        Merchant merchant = authFacade.currentMerchant();
        if (key != null && !key.isBlank()) {
            var existing = orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), key);
            if (existing.isPresent()) return toDto(existing.get());
        }
        Order order = new Order(); order.setId(IdGenerator.next("ord_")); order.setMerchant(merchant); order.setAmount(request.amount()); order.setCurrency(request.currency() == null ? "INR" : request.currency().toUpperCase()); order.setNotes(request.notes()); order.setCustomerEmail(request.customerEmail()); order.setStatus(OrderStatus.CREATED); order.setExpiresAt(Instant.now().plusSeconds((request.expiresInMinutes() == null ? 15 : request.expiresInMinutes()) * 60L)); order.setIdempotencyKey(key); return toDto(orderRepository.save(order));
    }
    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderDto> list(int page, int size) {
        Merchant merchant = authFacade.currentMerchant(); var result = orderRepository.findByMerchantId(merchant.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.getContent().stream().map(this::toDto).toList(), page, size, result.getTotalElements());
    }
    @Transactional(readOnly = true)
    public OrderDtos.OrderDetailDto detail(String id) {
        Order order = owned(id); return new OrderDtos.OrderDetailDto(toDto(order), paymentRepository.findByOrderIdOrderByCreatedAtDesc(id).stream().map(this::summary).toList());
    }
    @Transactional
    public Order getForPayment(String id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order was not found"));
        if (order.getStatus() == OrderStatus.CREATED && order.getExpiresAt().isBefore(Instant.now())) { order.setStatus(OrderStatus.EXPIRED); orderRepository.save(order); }
        return order;
    }
    @Transactional(readOnly = true)
    public OrderDtos.PublicOrderDto publicDetail(String id) {
        Order order = getForPayment(id); Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(id).stream().findFirst().orElse(null);
        long remaining = Math.max(0, Duration.between(Instant.now(), order.getExpiresAt()).getSeconds());
        return new OrderDtos.PublicOrderDto(order.getId(), order.getMerchant().getBusinessName(), order.getAmount(), order.getCurrency(), order.getNotes(), order.getStatus(), order.getExpiresAt(), remaining, order.getStatus() == OrderStatus.CREATED && remaining > 0, payment == null ? null : publicPayment(payment));
    }
    public Order owned(String id) { return orderRepository.findByIdAndMerchantId(id, authFacade.currentMerchant().getId()).orElseThrow(() -> new NotFoundException("Order was not found")); }
    public OrderDtos.OrderDto toDto(Order order) { Payment p = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().findFirst().orElse(null); return new OrderDtos.OrderDto(order.getId(), order.getAmount(), order.getCurrency(), order.getNotes(), order.getCustomerEmail(), order.getStatus(), order.getExpiresAt(), order.getCreatedAt(), "/pay/" + order.getId(), p == null ? null : p.getId(), p == null ? null : p.getStatus()); }
    public OrderDtos.PaymentSummaryDto summary(Payment p) { return new OrderDtos.PaymentSummaryDto(p.getId(), p.getOrder().getId(), p.getAmount(), p.getCurrency(), p.getMethod(), p.getMethodSummary(), p.getStatus(), null, null, p.getCustomerEmail(), p.getRefundedAmount(), p.getCreatedAt()); }
    public OrderDtos.PublicPaymentDto publicPayment(Payment p) { return new OrderDtos.PublicPaymentDto(p.getId(), p.getOrder().getId(), p.getStatus(), p.getMethod(), p.getMethodSummary(), p.getAmount(), p.getCurrency(), p.getFailureReason(), p.getStatus() == PaymentStatus.VERIFICATION_REQUIRED, p.getCreatedAt()); }
}
