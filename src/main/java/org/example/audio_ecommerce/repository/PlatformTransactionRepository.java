package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlatformTransactionRepository extends JpaRepository<PlatformTransaction, UUID> {

    List<PlatformTransaction> findAllByTypeAndStatus(TransactionType type, TransactionStatus status);
    // Lọc theo storeId
    List<PlatformTransaction> findByStoreId(UUID storeId);

    // Lọc theo trạng thái
    List<PlatformTransaction> findByStatus(TransactionStatus status);

    // Lọc theo loại giao dịch
    List<PlatformTransaction> findByType(TransactionType type);

    // Lọc theo ngày tạo (from - to)
    @Query("SELECT t FROM PlatformTransaction t WHERE t.createdAt BETWEEN :from AND :to")
    List<PlatformTransaction> findByDateRange(LocalDateTime from, LocalDateTime to);

    // Lọc theo storeId + status + type + date (tùy chọn)
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
            UUID storeId,
            UUID customerId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to
    );

    List<PlatformTransaction> findAllByOrderIdAndStatus(UUID orderId, TransactionStatus status);

    @Query("SELECT t FROM PlatformTransaction t " +
            "WHERE t.status = org.example.audio_ecommerce.entity.Enum.TransactionStatus.PENDING " +
            "AND t.createdAt < :threshold")
    List<PlatformTransaction> findExpiredHoldings(LocalDateTime threshold);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM PlatformTransaction t
        WHERE (:type IS NULL OR t.type = :type)
          AND (:status IS NULL OR t.status = :status)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to   IS NULL OR t.createdAt <  :to)
    """)
    BigDecimal sumAmountByTypeAndFilter(@Param("type") TransactionType type,
                                        @Param("status") TransactionStatus status,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // ✅ Check trùng transaction theo idempotencyKey
    boolean existsByIdempotencyKey(String idempotencyKey);

    // ✅ Đếm số lượng transaction theo status và type
    @Query("""
        SELECT COUNT(t)
        FROM PlatformTransaction t
        WHERE t.status = :status AND t.type = :type
    """)
    Long countByStatusAndType(
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type
    );

}
