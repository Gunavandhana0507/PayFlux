package com.payflux.api.service;

import com.payflux.api.domain.AppUser;
import com.payflux.api.domain.Merchant;
import com.payflux.api.domain.UserRole;
import com.payflux.api.dto.AuthResponse;
import com.payflux.api.dto.LoginRequest;
import com.payflux.api.dto.MerchantProfileResponse;
import com.payflux.api.dto.RegisterRequest;
import com.payflux.api.repo.AppUserRepository;
import com.payflux.api.repo.MerchantRepository;
import com.payflux.api.security.JwtService;
import com.payflux.api.web.ApiException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(UserRole.MERCHANT);
        userRepository.save(user);

        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setBusinessName(request.businessName());
        merchant.setLegalName(request.legalName());
        merchant.setBusinessType(request.businessType());
        merchant.setWebsiteUrl(request.websiteUrl());
        merchant.setContactName(request.contactName());
        merchant.setContactEmail(user.getEmail());
        merchant.setContactPhone(request.contactPhone());
        merchant.setPanNumber(request.panNumber());
        merchant.setGstin(request.gstin());
        merchant.setAddressLine1(request.addressLine1());
        merchant.setAddressLine2(request.addressLine2());
        merchant.setCity(request.city());
        merchant.setState(request.state());
        merchant.setPostalCode(request.postalCode());
        merchant.setCountry(request.country());
        merchant.setBankAccountName(request.bankAccountName());
        merchant.setBankAccountNumber(request.bankAccountNumber());
        merchant.setBankIfsc(request.bankIfsc());
        merchant.setApiKey("pk_test_" + UUID.randomUUID().toString().replace("-", ""));
        merchantRepository.save(merchant);

        return buildAuthResponse(user, merchant);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository
                .findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        Merchant merchant = merchantRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> ApiException.unauthorized("No merchant account for this user"));
        return buildAuthResponse(user, merchant);
    }

    private AuthResponse buildAuthResponse(AppUser user, Merchant merchant) {
        String token = jwtService.issueToken(
                user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, jwtService.expirySeconds(), MerchantProfileResponse.from(merchant));
    }
}
