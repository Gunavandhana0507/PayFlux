package com.payflux.api.config;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflux")
@Getter
@Setter
public class PayFluxProperties {

    /** Base URL of the checkout UI, used to build the shareable payment link. */
    private String checkoutBaseUrl = "http://localhost:5173";

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Order order = new Order();
    private MockProcessor mockProcessor = new MockProcessor();
    private Fraud fraud = new Fraud();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expiryMinutes = 720;
    }

    @Getter
    @Setter
    public static class Order {
        private long expiryMinutes = 15;
    }

    @Getter
    @Setter
    public static class MockProcessor {
        private String defaultOutcome = "SUCCESS";
        private long latencyMs = 250;
        private double successRate = 0.85;
    }

    @Getter
    @Setter
    public static class Fraud {
        private BigDecimal highAmountThreshold = new BigDecimal("50000");
        private BigDecimal veryHighAmountThreshold = new BigDecimal("200000");
        private int failedAttemptWindowMinutes = 10;
        private int failedAttemptThreshold = 3;
        private int mediumRiskScore = 30;
        private int highRiskScore = 70;
    }
}
