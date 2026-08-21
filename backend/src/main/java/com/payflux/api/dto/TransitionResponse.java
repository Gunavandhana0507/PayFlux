package com.payflux.api.dto;

import com.payflux.api.domain.PaymentTransitionLog;
import java.time.Instant;

public record TransitionResponse(String fromStatus, String toStatus, String reason, String actor, Instant createdAt) {

    public static TransitionResponse from(PaymentTransitionLog log) {
        return new TransitionResponse(
                log.getFromStatus() == null ? null : log.getFromStatus().name(),
                log.getToStatus().name(),
                log.getReason(),
                log.getActor(),
                log.getCreatedAt());
    }
}
