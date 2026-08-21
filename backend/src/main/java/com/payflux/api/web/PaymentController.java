package com.payflux.api.web;

import com.payflux.api.domain.PaymentStatus;
import com.payflux.api.dto.PageResponse;
import com.payflux.api.dto.PaymentDetailResponse;
import com.payflux.api.dto.PaymentResponse;
import com.payflux.api.dto.RefundRequest;
import com.payflux.api.dto.RefundResponse;
import com.payflux.api.service.CurrentUserService;
import com.payflux.api.service.PaymentService;
import com.payflux.api.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    public PaymentController(
            PaymentService paymentService, RefundService refundService, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var merchant = currentUserService.requireMerchant();
        var result = paymentService.listForMerchant(merchant.getId(), status, page, size);
        var responses = paymentService.toResponses(result.getContent());
        return new PageResponse<>(
                responses, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{paymentId}")
    public PaymentDetailResponse detail(@PathVariable String paymentId) {
        var merchant = currentUserService.requireMerchant();
        return paymentService.getDetailForMerchant(merchant.getId(), paymentId);
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable String paymentId, @Valid @RequestBody RefundRequest request) {
        var merchant = currentUserService.requireMerchant();
        var user = currentUserService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.create(merchant.getId(), paymentId, request, user.email()));
    }
}
