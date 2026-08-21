package com.payflux.api.dto;

/**
 * One contributing factor behind a risk score. Shaped like the explanation a real
 * model service would return (feature code, human-readable reason, contribution).
 */
public record RiskFactorDto(String code, String description, int weight) {}
