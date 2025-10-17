package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ShopVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopVoucherRepository extends JpaRepository<ShopVoucher, UUID> {
    Optional<ShopVoucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
