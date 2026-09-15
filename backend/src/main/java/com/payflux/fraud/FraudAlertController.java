package com.payflux.fraud;

import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.common.PageResponse;
import com.payflux.order.OrderDtos;
import com.payflux.payment.Payment;
import com.payflux.payment.PaymentStatus;
import com.payflux.security.AuthFacade;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {
    private final FraudAnalysisRepository repository;
    private final FraudService fraudService;
    private final AuthFacade authFacade;
    public FraudAlertController(FraudAnalysisRepository repository, FraudService fraudService, AuthFacade authFacade) { this.repository = repository; this.fraudService = fraudService; this.authFacade = authFacade; }
    @GetMapping public PageResponse<OrderDtos.PaymentSummaryDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { var result = repository.findByPaymentMerchantIdAndRiskLevelIn(authFacade.currentMerchant().getId(), List.of(RiskLevel.MEDIUM, RiskLevel.HIGH), PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))); return new PageResponse<>(result.getContent().stream().map(this::summary).toList(), page, size, result.getTotalElements()); }
    @PostMapping("/{id}/feedback") public FraudDtos.FraudAnalysisDto feedback(@PathVariable String id, @Valid @RequestBody FraudDtos.FeedbackRequest request) { FraudAnalysis analysis = repository.findById(id).filter(a -> a.getPayment().getMerchant().getId().equals(authFacade.currentMerchant().getId())).orElseThrow(() -> new NotFoundException("Fraud analysis was not found")); analysis.setMerchantFeedback(request.feedback()); analysis.setFeedbackNote(request.note()); analysis.setFeedbackAt(Instant.now()); analysis.setFeedbackByUserId(authFacade.currentUserId()); return fraudService.toDto(repository.save(analysis)); }
    private OrderDtos.PaymentSummaryDto summary(FraudAnalysis a) { Payment p = a.getPayment(); return new OrderDtos.PaymentSummaryDto(p.getId(), p.getOrder().getId(), p.getAmount(), p.getCurrency(), p.getMethod(), p.getMethodSummary(), p.getStatus(), a.getRiskLevel(), a.getRiskScore(), p.getCustomerEmail(), p.getRefundedAmount(), p.getCreatedAt()); }
}
