package com.payflux.api.service;

import com.payflux.api.domain.Merchant;
import com.payflux.api.repo.MerchantRepository;
import com.payflux.api.security.AuthenticatedUser;
import com.payflux.api.web.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final MerchantRepository merchantRepository;

    public CurrentUserService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public AuthenticatedUser requireUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ApiException.unauthorized("Not authenticated");
        }
        return user;
    }

    public Merchant requireMerchant() {
        AuthenticatedUser user = requireUser();
        return merchantRepository
                .findByUserId(user.userId())
                .orElseThrow(() -> ApiException.unauthorized("No merchant account for this user"));
    }
}
