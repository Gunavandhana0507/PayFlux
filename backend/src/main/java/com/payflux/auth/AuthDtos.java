package com.payflux.auth;

import com.payflux.merchant.MerchantStatus;

import jakarta.validation.constraints.*;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String businessName,
            @NotBlank String businessType,
            @NotBlank
                    @Pattern(
                            regexp = "^[0-9]{2}[A-Z0-9]{13}$",
                            message = "Enter a valid 15-character GST ID")
                    String gstId,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record MerchantDto(
            Long id,
            String businessName,
            String businessType,
            String gstId,
            String email,
            MerchantStatus status,
            Instant createdAt) {}

    public record AuthResponse(String token, MerchantDto merchant) {}
}
