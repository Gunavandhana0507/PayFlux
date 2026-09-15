package com.payflux.order;

import com.payflux.payment.PaymentDtos;
import com.payflux.payment.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicOrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    public PublicOrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/orders/{id}")
    public OrderDtos.PublicOrderDto detail(@PathVariable String id) {
        return orderService.publicDetail(id);
    }

    @PostMapping("/orders/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.PublicPaymentDto pay(
            @PathVariable String id,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String key,
            @Valid @RequestBody PaymentDtos.PaymentRequest request) {
        return paymentService.createPublic(id, key, request);
    }

    @PostMapping("/payments/{id}/verify")
    public OrderDtos.PublicPaymentDto verify(
            @PathVariable String id, @Valid @RequestBody PaymentDtos.VerifyRequest request) {
        return paymentService.verify(id, request);
    }

    @GetMapping("/payments/{id}")
    public OrderDtos.PublicPaymentDto payment(@PathVariable String id) {
        return paymentService.publicPayment(id);
    }
}
