package com.payflux.security;

import com.payflux.common.ApiExceptions.NotFoundException;
import com.payflux.merchant.Merchant;
import com.payflux.merchant.MerchantRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {
    private final MerchantRepository merchantRepository;

    public AuthFacade(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String))
            throw new NotFoundException("Authenticated user was not found");
        return Long.valueOf((String) auth.getPrincipal());
    }

    public Merchant currentMerchant() {
        return merchantRepository
                .findByUserId(currentUserId())
                .orElseThrow(() -> new NotFoundException("Merchant was not found"));
    }
}
