package com.payflux.api.web;

import com.payflux.api.dto.InitiatePaymentRequest;
import com.payflux.api.dto.PublicOrderResponse;
import com.payflux.api.dto.PublicPaymentResponse;
import com.payflux.api.dto.VerifyPaymentRequest;
import com.payflux.api.service.OrderService;
import com.payflux.api.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated endpoints backing the hosted checkout page at /pay/:orderId. */
@RestController
@RequestMapping("/api/public")
public class PublicCheckoutController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public PublicCheckoutController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/orders/{orderId}")
    public PublicOrderResponse getOrder(@PathVariable String orderId) {
        return PublicOrderResponse.from(orderService.getPublic(orderId));
    }

    @PostMapping("/payments")
    public PublicPaymentResponse initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        var order = orderService.getPublic(request.orderId());
        return paymentService.initiate(order, request, httpRequest.getRemoteAddr(), idempotencyKey);
    }

    @PostMapping("/payments/{paymentId}/verify")
    public PublicPaymentResponse verify(
            @PathVariable String paymentId, @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verify(paymentId, request.otp());
    }

    @GetMapping("/payments/{paymentId}")
    public PublicPaymentResponse get(@PathVariable String paymentId) {
        return paymentService.getPublic(paymentId);
    }
}
