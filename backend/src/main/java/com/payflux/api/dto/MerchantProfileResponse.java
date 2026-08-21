package com.payflux.api.dto;

import com.payflux.api.domain.Merchant;

public record MerchantProfileResponse(
        String merchantId,
        String userId,
        String email,
        String fullName,
        String role,
        String businessName,
        String legalName,
        String businessType,
        String websiteUrl,
        String contactName,
        String contactPhone,
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
        String bankIfsc,
        String kycStatus,
        String apiKey) {

    public static MerchantProfileResponse from(Merchant merchant) {
        var user = merchant.getUser();
        return new MerchantProfileResponse(
                merchant.getId(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                merchant.getBusinessName(),
                merchant.getLegalName(),
                merchant.getBusinessType(),
                merchant.getWebsiteUrl(),
                merchant.getContactName(),
                merchant.getContactPhone(),
                merchant.getPanNumber(),
                merchant.getGstin(),
                merchant.getAddressLine1(),
                merchant.getAddressLine2(),
                merchant.getCity(),
                merchant.getState(),
                merchant.getPostalCode(),
                merchant.getCountry(),
                merchant.getBankAccountName(),
                merchant.getBankAccountNumber(),
                merchant.getBankIfsc(),
                merchant.getKycStatus().name(),
                merchant.getApiKey());
    }
}
