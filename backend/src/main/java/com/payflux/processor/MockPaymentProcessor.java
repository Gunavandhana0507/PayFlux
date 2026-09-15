package com.payflux.processor;

import com.payflux.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProcessor {
    private final ProcessorOutcome defaultOutcome;
    public MockPaymentProcessor(@Value("${payflux.processor.default-outcome}") ProcessorOutcome defaultOutcome) { this.defaultOutcome = defaultOutcome; }
    public ProcessorResult process(Payment payment, ProcessorOutcome override) {
        ProcessorOutcome outcome = override == null ? deterministic(payment) : override;
        if (outcome == null) outcome = defaultOutcome;
        if (outcome == ProcessorOutcome.TIMEOUT) { try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return new ProcessorResult(outcome, null, "The payment took too long and was cancelled. Nothing was charged."); }
        if (outcome == ProcessorOutcome.FAILURE) return new ProcessorResult(outcome, null, "Your bank declined this payment. Try another method.");
        return new ProcessorResult(outcome, "mock_" + UUID.randomUUID().toString().replace("-", ""), null);
    }
    private ProcessorOutcome deterministic(Payment payment) {
        String summary = payment.getMethodSummary() == null ? "" : payment.getMethodSummary();
        if (payment.getMethod() == com.payflux.payment.PaymentMethod.CARD) { if (summary.endsWith("0000")) return ProcessorOutcome.FAILURE; if (summary.endsWith("9999")) return ProcessorOutcome.TIMEOUT; }
        if (payment.getMethod() == com.payflux.payment.PaymentMethod.UPI) { if (summary.startsWith("UPI fail@")) return ProcessorOutcome.FAILURE; if (summary.startsWith("UPI slow@")) return ProcessorOutcome.TIMEOUT; }
        return null;
    }
    public record ProcessorResult(ProcessorOutcome outcome, String processorRef, String message) {}
}
