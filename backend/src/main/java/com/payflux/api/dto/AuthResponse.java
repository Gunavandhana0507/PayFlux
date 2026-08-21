package com.payflux.api.dto;

public record AuthResponse(String token, long expiresIn, MerchantProfileResponse profile) {}
