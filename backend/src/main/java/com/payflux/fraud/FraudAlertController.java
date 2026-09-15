package com.payflux.fraud;

import com.payflux.common.PageResponse;
import com.payflux.order.OrderDtos;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {
    private final FraudService fraudService;

    public FraudAlertController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping
    public PageResponse<OrderDtos.PaymentSummaryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return fraudService.listAlerts(page, Math.min(size, 100));
    }

    @PostMapping("/{id}/feedback")
    public FraudDtos.FraudAnalysisDto feedback(
            @PathVariable String id, @Valid @RequestBody FraudDtos.FeedbackRequest request) {
        return fraudService.submitFeedback(id, request);
    }
}
