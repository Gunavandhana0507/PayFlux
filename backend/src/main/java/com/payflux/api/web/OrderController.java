package com.payflux.api.web;

import com.payflux.api.domain.Merchant;
import com.payflux.api.dto.CreateOrderRequest;
import com.payflux.api.dto.OrderResponse;
import com.payflux.api.dto.PageResponse;
import com.payflux.api.dto.PaymentResponse;
import com.payflux.api.service.CurrentUserService;
import com.payflux.api.service.OrderService;
import com.payflux.api.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public OrderController(
            OrderService orderService, PaymentService paymentService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Merchant merchant = currentUserService.requireMerchant();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(merchant, request, idempotencyKey));
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Merchant merchant = currentUserService.requireMerchant();
        return PageResponse.of(orderService.listForMerchant(merchant.getId(), page, size), orderService::toResponse);
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> get(@PathVariable String orderId) {
        Merchant merchant = currentUserService.requireMerchant();
        var order = orderService.getForMerchant(merchant.getId(), orderId);
        List<PaymentResponse> payments = paymentService.toResponses(paymentService.listForOrder(orderId));
        return Map.of("order", orderService.toResponse(order), "payments", payments);
    }
}
