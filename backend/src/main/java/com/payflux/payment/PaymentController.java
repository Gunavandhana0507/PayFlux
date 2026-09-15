package com.payflux.payment;

import com.payflux.common.PageResponse;
import com.payflux.fraud.RiskLevel;
import com.payflux.order.OrderDtos;
import com.payflux.refund.RefundDtos;
import com.payflux.refund.RefundService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final RefundService refundService;

    public PaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @GetMapping
    public PageResponse<OrderDtos.PaymentSummaryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) PaymentMethod method) {
        return paymentService.list(page, Math.min(size, 100), status, riskLevel, method);
    }

    @GetMapping("/{id}")
    public PaymentDtos.PaymentDetailDto detail(@PathVariable String id) {
        return paymentService.detail(id);
    }

    @PostMapping("/{id}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundDtos.RefundDto refund(
            @PathVariable String id, @Valid @RequestBody RefundDtos.CreateRefundRequest request) {
        return refundService.create(id, request);
    }
}
