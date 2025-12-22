package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformTransactionRepository extends JpaRepository<PlatformTransaction, UUID> {

    // ===== Basic queries (tuỳ bạn giữ hay xoá) =====
    List<PlatformTransaction> findAllByTypeAndStatus(TransactionType type, TransactionStatus status);

    List<PlatformTransaction> findByStoreId(UUID storeId);

    List<PlatformTransaction> findByStatus(TransactionStatus status);

    List<PlatformTransaction> findByType(TransactionType type);

    List<PlatformTransaction> findAllByOrderIdAndStatus(UUID orderId, TransactionStatus status);

    // ===== Date range =====
    @Query("SELECT t FROM PlatformTransaction t WHERE t.createdAt BETWEEN :from AND :to")
    List<PlatformTransaction> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ===== Filter (list) - giữ tương thích code cũ =====
    @Query("""
        SELECT t FROM PlatformTransaction t 
        WHERE (:storeId IS NULL OR t.storeId = :storeId)
          AND (:customerId IS NULL OR t.customerId = :customerId)
          AND (:status IS NULL OR t.status = :status)
          AND (:type IS NULL OR t.type = :type)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to IS NULL OR t.createdAt <= :to)
        ORDER BY t.createdAt DESC
    """)
    List<PlatformTransaction> filterTransactions(
            @Param("storeId") UUID storeId,
            @Param("customerId") UUID customerId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ===== Find expired holdings =====
    @Query("""
        SELECT t FROM PlatformTransaction t
        WHERE t.status = org.example.audio_ecommerce.entity.Enum.TransactionStatus.PENDING
          AND t.createdAt < :threshold
    """)
    List<PlatformTransaction> findExpiredHoldings(@Param("threshold") LocalDateTime threshold);

    // ===== Sum =====
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM PlatformTransaction t
        WHERE (:type IS NULL OR t.type = :type)
          AND (:status IS NULL OR t.status = :status)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to   IS NULL OR t.createdAt <  :to)
    """)
    BigDecimal sumAmountByTypeAndFilter(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ===== Idempotency =====
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<PlatformTransaction> findByIdempotencyKey(String idempotencyKey);

    // ===== Count by status & type =====
    @Query("""
        SELECT COUNT(t)
        FROM PlatformTransaction t
        WHERE t.status = :status AND t.type = :type
    """)
    Long countByStatusAndType(
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type
    );

    // ==========================================================
    // ✅ MAIN: Filter transactions of ONLY the flat wallet (PLATFORM)
    // ==========================================================
    @Query("""
        select tx from PlatformTransaction tx
        where tx.wallet.ownerType = org.example.audio_ecommerce.entity.Enum.WalletOwnerType.PLATFORM

          and (:storeId is null or tx.storeId = :storeId)
          and (:customerId is null or tx.customerId = :customerId)
          and (:orderId is null or tx.orderId = :orderId)
          and (:payoutRequestId is null or tx.payoutRequestId = :payoutRequestId)

          and (:status is null or tx.status = :status)
          and (:type is null or tx.type = :type)
          and (:bucket is null or tx.bucket = :bucket)
          and (:direction is null or tx.direction = :direction)
          and (:channel is null or tx.channel = :channel)

          and (:from is null or tx.createdAt >= :from)
          and (:to is null or tx.createdAt <= :to)
        order by tx.createdAt desc
    """)
    Page<PlatformTransaction> filterFlatWalletTransactions(
            @Param("storeId") UUID storeId,
            @Param("customerId") UUID customerId,
            @Param("orderId") UUID orderId,
            @Param("payoutRequestId") UUID payoutRequestId,

            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("bucket") WalletBucket bucket,
            @Param("direction") TxDirection direction,
            @Param("channel") PaymentChannel channel,

            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
    select tx from PlatformTransaction tx
    where tx.wallet.ownerType = org.example.audio_ecommerce.entity.Enum.WalletOwnerType.PLATFORM
      and (:type is null or tx.type = :type)
      and (:status is null or tx.status = :status)
      and (:from is null or tx.createdAt >= :from)
      and (:to is null or tx.createdAt <= :to)
    order by tx.createdAt desc
""")
    Page<PlatformTransaction> findFlatWalletTransactions(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
