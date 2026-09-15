package com.payflux.payment;

import com.payflux.fraud.FraudDtos;
import com.payflux.order.OrderDtos;
import com.payflux.processor.ProcessorOutcome;
import com.payflux.refund.RefundDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record CardDetails(
            @NotBlank String number,
            @NotNull @Min(1) @Max(12) Integer expiryMonth,
            @NotNull @Min(2024) Integer expiryYear,
            @NotBlank String holderName) {}

    public record PaymentRequest(
            @NotNull PaymentMethod method,
            @NotBlank @Email String customerEmail,
            @NotBlank String deviceId,
            ProcessorOutcome simulateOutcome,
            @Valid CardDetails card,
            @Pattern(regexp = "^[A-Za-z0-9_.-]+@[A-Za-z0-9_]+$") String upiId,
            String bankCode,
            String walletProvider) {}

    public record VerifyRequest(@NotBlank String otp) {}

    public record PaymentDetailDto(
            OrderDtos.PaymentSummaryDto payment,
            String failureReason,
            String deviceId,
            String processorRef,
            OrderDtos.OrderDto order,
            FraudDtos.FraudAnalysisDto fraudAnalysis,
            List<RefundDtos.RefundDto> refunds,
            List<TransitionDto> transitions,
            BigDecimal refundableAmount) {}

    public record TransitionDto(
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            TransitionActor actor,
            String reason,
            Instant createdAt) {}
}
