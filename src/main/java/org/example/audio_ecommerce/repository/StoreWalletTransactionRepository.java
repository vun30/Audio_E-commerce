package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoreWalletTransactionRepository extends JpaRepository<StoreWalletTransaction, UUID> {

    // 🔍 Lấy tất cả giao dịch theo wallet
    List<StoreWalletTransaction> findByWallet(StoreWallet wallet);

    // 🔍 Phân trang giao dịch của 1 ví
    Page<StoreWalletTransaction> findByWallet_WalletId(UUID walletId, Pageable pageable);

    // 🔍 Lấy giao dịch gần nhất của ví (dễ dùng khi cần kiểm tra số dư cuối)
    StoreWalletTransaction findTop1ByWallet_WalletIdOrderByCreatedAtDesc(UUID walletId);

    Page<StoreWalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    Page<StoreWalletTransaction> findByWallet_WalletIdAndTypeOrderByCreatedAtDesc(
            UUID walletId, StoreWalletTransactionType type, Pageable pageable);

    @Query("""
    SELECT t FROM StoreWalletTransaction t
    WHERE t.wallet.id = :walletId
      AND (:from IS NULL OR t.createdAt >= :from)
      AND (:to IS NULL OR t.createdAt <= :to)
      AND (:type IS NULL OR t.type = :type)
      AND (:transactionId IS NULL OR t.transactionId = :transactionId)
    ORDER BY t.createdAt DESC
    """)
    Page<StoreWalletTransaction> filterTransactions(
            @Param("walletId") UUID walletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") StoreWalletTransactionType type,
            @Param("transactionId") UUID transactionId,
            Pageable pageable
    );

}
