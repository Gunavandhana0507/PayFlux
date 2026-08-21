package com.payflux.api.repo;

import com.payflux.api.domain.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByUserId(String userId);
}
