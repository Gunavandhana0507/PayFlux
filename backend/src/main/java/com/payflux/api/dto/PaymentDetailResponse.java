package com.payflux.api.dto;

import java.util.List;

public record PaymentDetailResponse(
        PaymentResponse payment,
        FraudAnalysisResponse fraudAnalysis,
        List<TransitionResponse> transitions,
        List<RefundResponse> refunds) {}
