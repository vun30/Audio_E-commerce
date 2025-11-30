package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.WalletTransaction;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWallet_Customer_IdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Optional<WalletTransaction> findFirstByWallet_Customer_IdAndOrderIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID customerId, UUID orderId, WalletTransactionType type);

    Optional<WalletTransaction> findByExternalRef(String externalRef);
}
