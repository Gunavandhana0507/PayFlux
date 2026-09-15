package com.payflux.auth;

import com.payflux.common.ApiExceptions.ConflictException;
import com.payflux.merchant.Merchant;
import com.payflux.merchant.MerchantRepository;
import com.payflux.merchant.MerchantStatus;
import com.payflux.security.AuthFacade;
import com.payflux.security.JwtUtil;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthFacade authFacade;

    public AuthService(
            AppUserRepository userRepository,
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthFacade authFacade) {
        this.userRepository = userRepository;
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authFacade = authFacade;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email()))
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        AppUser user = new AppUser();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.MERCHANT);
        user = userRepository.save(user);
        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setBusinessName(request.businessName());
        merchant.setBusinessType(request.businessType());
        merchant.setGstId(request.gstId());
        merchant.setStatus(MerchantStatus.ACTIVE);
        merchant = merchantRepository.save(merchant);
        return new AuthDtos.AuthResponse(jwtUtil.generate(user, merchant), toDto(merchant));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        AppUser user =
                userRepository
                        .findByEmailIgnoreCase(request.email())
                        .orElseThrow(() -> new BadCredentialsException("invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new BadCredentialsException("invalid credentials");
        Merchant merchant = merchantRepository.findByUserId(user.getId()).orElse(null);
        return new AuthDtos.AuthResponse(
                jwtUtil.generate(user, merchant), merchant == null ? null : toDto(merchant));
    }

    @Transactional(readOnly = true)
    public AuthDtos.MerchantDto me() {
        return toDto(authFacade.currentMerchant());
    }

    public AuthDtos.MerchantDto toDto(Merchant merchant) {
        return new AuthDtos.MerchantDto(
                merchant.getId(),
                merchant.getBusinessName(),
                merchant.getBusinessType(),
                merchant.getGstId(),
                merchant.getUser().getEmail(),
                merchant.getStatus(),
                merchant.getCreatedAt());
    }
}
