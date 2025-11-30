package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByCustomer_Id(UUID customerId);
    Optional<Wallet> findByCustomer_Id(UUID customerId);
    // Dùng cho cập nhật số dư an toàn
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.customer.id = :cid")
    Optional<Wallet> findByCustomerIdForUpdate(@Param("cid") UUID customerId);
}
