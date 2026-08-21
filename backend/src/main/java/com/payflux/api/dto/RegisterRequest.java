package com.payflux.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Merchant sign-up: login credentials plus the KYC/onboarding fields. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String fullName,
        @NotBlank String businessName,
        String legalName,
        String businessType,
        String websiteUrl,
        @NotBlank String contactName,
        @NotBlank String contactPhone,
        String panNumber,
        String gstin,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String bankAccountName,
        String bankAccountNumber,
        String bankIfsc) {}
