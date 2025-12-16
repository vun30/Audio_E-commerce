package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.WalletTransaction;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWallet_Customer_IdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Optional<WalletTransaction> findFirstByWallet_Customer_IdAndOrderIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID customerId, UUID orderId, WalletTransactionType type);

    Optional<WalletTransaction> findByExternalRef(String externalRef);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM WalletTransaction t
        WHERE (:customerId IS NULL OR t.wallet.customer.id = :customerId)
          AND (:type IS NULL OR t.transactionType = :type)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to   IS NULL OR t.createdAt <  :to)
    """)
    BigDecimal sumAmountByTypeAndFilter(@Param("customerId") UUID customerId,
                                        @Param("type") WalletTransactionType type,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);
}
