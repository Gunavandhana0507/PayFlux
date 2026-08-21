package com.payflux.api.web;

import com.payflux.api.domain.RiskLevel;
import com.payflux.api.dto.FraudAnalysisResponse;
import com.payflux.api.dto.FraudFeedbackRequest;
import com.payflux.api.dto.PageResponse;
import com.payflux.api.repo.FraudAnalysisRepository;
import com.payflux.api.service.CurrentUserService;
import com.payflux.api.service.FraudService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {

    private final FraudService fraudService;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final CurrentUserService currentUserService;

    public FraudAlertController(
            FraudService fraudService,
            FraudAnalysisRepository fraudAnalysisRepository,
            CurrentUserService currentUserService) {
        this.fraudService = fraudService;
        this.fraudAnalysisRepository = fraudAnalysisRepository;
        this.currentUserService = currentUserService;
    }

    /** Flagged (Medium/High risk) analyses for the signed-in merchant. */
    @GetMapping
    public PageResponse<FraudAnalysisResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var merchant = currentUserService.requireMerchant();
        var result = fraudAnalysisRepository.findByPaymentOrderMerchantIdAndRiskLevelInOrderByCreatedAtDesc(
                merchant.getId(), List.of(RiskLevel.MEDIUM, RiskLevel.HIGH), PageRequest.of(page, size));
        return PageResponse.of(result, fraudService::toResponse);
    }

    /**
     * Merchant verdict on a flagged transaction. Accepts the fraud analysis id or the
     * payment id, and stores the verdict against the analysis' original feature set.
     */
    @PostMapping("/{id}/feedback")
    public FraudAnalysisResponse feedback(@PathVariable String id, @Valid @RequestBody FraudFeedbackRequest request) {
        var merchant = currentUserService.requireMerchant();
        var user = currentUserService.requireUser();
        return fraudService.recordFeedback(merchant.getId(), id, user.email(), request);
    }
}
