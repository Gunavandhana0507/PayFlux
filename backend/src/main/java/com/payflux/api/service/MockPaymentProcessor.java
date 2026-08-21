package com.payflux.api.service;

import com.payflux.api.config.PayFluxProperties;
import com.payflux.api.domain.Payment;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stand-in for an acquiring bank / PSP. The outcome is configurable: an explicit
 * {@code simulateOutcome} on the request wins, otherwise the configured default
 * ({@code payflux.mock-processor.default-outcome}, where RANDOM uses the success rate).
 */
@Service
public class MockPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);

    private final PayFluxProperties properties;

    public MockPaymentProcessor(PayFluxProperties properties) {
        this.properties = properties;
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }

    public record ProcessorResult(Outcome outcome, String reference, String message, String failureCode) {}

    public ProcessorResult authorize(Payment payment, String simulateOutcome) {
        sleepForLatency();
        Outcome outcome = resolveOutcome(simulateOutcome);
        String reference = "mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        log.info("Mock processor authorize payment={} method={} outcome={}", payment.getId(), payment.getMethod(), outcome);
        return switch (outcome) {
            case SUCCESS -> new ProcessorResult(outcome, reference, "Authorized by mock processor", null);
            case FAILURE -> new ProcessorResult(
                    outcome, reference, "Declined by issuing bank", "ISSUER_DECLINED");
            case TIMEOUT -> new ProcessorResult(
                    outcome, reference, "No response from processor within the timeout window", "PROCESSOR_TIMEOUT");
        };
    }

    public String refund(Payment payment) {
        sleepForLatency();
        return "mock_rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private Outcome resolveOutcome(String simulateOutcome) {
        String requested = simulateOutcome == null || simulateOutcome.isBlank()
                ? properties.getMockProcessor().getDefaultOutcome()
                : simulateOutcome;
        String normalized = requested.trim().toUpperCase(Locale.ROOT);
        if ("RANDOM".equals(normalized)) {
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < properties.getMockProcessor().getSuccessRate()) {
                return Outcome.SUCCESS;
            }
            return roll < properties.getMockProcessor().getSuccessRate() + 0.1 ? Outcome.TIMEOUT : Outcome.FAILURE;
        }
        try {
            return Outcome.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return Outcome.SUCCESS;
        }
    }

    private void sleepForLatency() {
        long latency = properties.getMockProcessor().getLatencyMs();
        if (latency <= 0) {
            return;
        }
        try {
            Thread.sleep(latency);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
