package com.payflux.api.repo;

import com.payflux.api.domain.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MerchantRepository extends JpaRepository<Merchant, String> {

    @Query("select m from Merchant m join fetch m.user where m.user.id = :userId")
    Optional<Merchant> findByUserId(@Param("userId") String userId);
}
