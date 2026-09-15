package com.payflux.order;

import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.IdGenerator;
import com.payflux.common.PageResponse;
import com.payflux.merchant.Merchant;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentMapper;
import com.payflux.payment.PaymentRepository;
import com.payflux.security.AuthFacade;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

@Service
public class OrderService {
    public record CreateResult(OrderDtos.OrderDto dto, boolean created) {}

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AuthFacade authFacade;
    private final PaymentMapper paymentMapper;

    public OrderService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            AuthFacade authFacade,
            PaymentMapper paymentMapper) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.authFacade = authFacade;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public CreateResult create(OrderDtos.CreateOrderRequest request, String key) {
        Merchant merchant = authFacade.currentMerchant();
        if (key != null && !key.isBlank()) {
            var existing = orderRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), key);
            if (existing.isPresent()) return new CreateResult(toDto(existing.get()), false);
        }
        Order order = new Order();
        order.setId(IdGenerator.next("ord_"));
        order.setMerchant(merchant);
        order.setAmount(request.amount());
        order.setCurrency(request.currency() == null ? "INR" : request.currency().toUpperCase());
        order.setNotes(request.notes());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CREATED);
        order.setExpiresAt(
                Instant.now()
                        .plusSeconds(
                                (request.expiresInMinutes() == null
                                                ? 15
                                                : request.expiresInMinutes())
                                        * 60L));
        order.setIdempotencyKey(key);
        return new CreateResult(toDto(orderRepository.save(order)), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderDto> list(
            int page, int size, OrderStatus status, LocalDate from, LocalDate to) {
        Merchant merchant = authFacade.currentMerchant();
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant =
                to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Specification<Order> specification =
                (root, query, criteriaBuilder) -> {
                    var predicates = new ArrayList<Predicate>();
                    predicates.add(
                            criteriaBuilder.equal(
                                    root.get("merchant").get("id"), merchant.getId()));
                    if (status != null) {
                        predicates.add(criteriaBuilder.equal(root.get("status"), status));
                    }
                    if (fromInstant != null) {
                        predicates.add(
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.get("createdAt"), fromInstant));
                    }
                    if (toInstant != null) {
                        predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toInstant));
                    }
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
        var result =
                orderRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderDetailDto detail(String id) {
        Order order = owned(id);
        return new OrderDtos.OrderDetailDto(
                toDto(order),
                paymentRepository.findByOrderIdOrderByCreatedAtDesc(id).stream()
                        .map(paymentMapper::toSummary)
                        .toList());
    }

    @Transactional
    public Order getForPayment(String id) {
        Order order =
                orderRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Order was not found"));
        if (order.getStatus() == OrderStatus.CREATED
                && order.getExpiresAt().isBefore(Instant.now())) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public OrderDtos.PublicOrderDto publicDetail(String id) {
        Order order = getForPayment(id);
        Payment payment =
                paymentRepository.findByOrderIdOrderByCreatedAtDesc(id).stream()
                        .findFirst()
                        .orElse(null);
        long remaining =
                Math.max(0, Duration.between(Instant.now(), order.getExpiresAt()).getSeconds());
        return new OrderDtos.PublicOrderDto(
                order.getId(),
                order.getMerchant().getBusinessName(),
                order.getAmount(),
                order.getCurrency(),
                order.getNotes(),
                order.getStatus(),
                order.getExpiresAt(),
                remaining,
                order.getStatus() == OrderStatus.CREATED && remaining > 0,
                payment == null ? null : paymentMapper.toPublic(payment));
    }

    public Order owned(String id) {
        return orderRepository
                .findByIdAndMerchantId(id, authFacade.currentMerchant().getId())
                .orElseThrow(() -> new NotFoundException("Order was not found"));
    }

    public OrderDtos.OrderDto toDto(Order order) {
        Payment p =
                paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                        .findFirst()
                        .orElse(null);
        return new OrderDtos.OrderDto(
                order.getId(),
                order.getAmount(),
                order.getCurrency(),
                order.getNotes(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                "/pay/" + order.getId(),
                p == null ? null : p.getId(),
                p == null ? null : p.getStatus());
    }
}
