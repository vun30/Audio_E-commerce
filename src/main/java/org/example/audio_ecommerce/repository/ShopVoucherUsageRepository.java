package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ShopVoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopVoucherUsageRepository extends JpaRepository<ShopVoucherUsage, UUID> {
    Optional<ShopVoucherUsage> findByVoucher_IdAndCustomer_Id(UUID voucherId, UUID customerId);
}
