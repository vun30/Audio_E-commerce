package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface StoreStatsRepository extends CrudRepository<org.example.audio_ecommerce.entity.StoreOrder, UUID> {

    @Query("""
            SELECT COUNT(s) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.createdAt >= :startOfDay AND s.createdAt < :endOfDay
            """)
    long countOrdersToday(@Param("storeId") UUID storeId,
                          @Param("startOfDay") java.time.LocalDateTime startOfDay,
                          @Param("endOfDay") java.time.LocalDateTime endOfDay);

    @Query("""
            SELECT COALESCE(SUM(s.grandTotal), 0) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.status = org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED
              AND s.deliveredAt >= :startOfDay AND s.deliveredAt < :endOfDay
            """)
    BigDecimal sumRevenueToday(@Param("storeId") UUID storeId,
                               @Param("startOfDay") java.time.LocalDateTime startOfDay,
                               @Param("endOfDay") java.time.LocalDateTime endOfDay);

    @Query("""
            SELECT COUNT(s) FROM StoreOrder s
            WHERE s.store.storeId = :storeId AND s.status = :status
            """)
    long countByStatus(@Param("storeId") UUID storeId, @Param("status") OrderStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM StoreRevenue r
            WHERE r.storeId = :storeId AND r.revenueDate >= :monthStart AND r.revenueDate <= :monthEnd
            """)
    BigDecimal sumRevenueThisMonth(@Param("storeId") UUID storeId,
                                   @Param("monthStart") LocalDate monthStart,
                                   @Param("monthEnd") LocalDate monthEnd);

    @Query("""
            SELECT COALESCE(SUM(r.amount - r.feePlatform - r.feeShipping), 0)
            FROM StoreRevenue r
            WHERE r.storeId = :storeId AND r.revenueDate >= :monthStart AND r.revenueDate <= :monthEnd
            """)
    BigDecimal sumProfitThisMonth(@Param("storeId") UUID storeId,
                                  @Param("monthStart") LocalDate monthStart,
                                  @Param("monthEnd") LocalDate monthEnd);

    @Query("""
            SELECT COALESCE(SUM(p.viewCount), 0) FROM Product p
            WHERE p.store.storeId = :storeId
            """)
    Long sumProductViews(@Param("storeId") UUID storeId);

    @Query("""
            SELECT COUNT(p) FROM Product p WHERE p.store.storeId = :storeId AND p.stockQuantity IS NOT NULL AND p.stockQuantity > 0
            """)
    long countProductsInStock(@Param("storeId") UUID storeId);

    @Query("""
            SELECT COUNT(p) FROM Product p WHERE p.store.storeId = :storeId AND (p.stockQuantity IS NULL OR p.stockQuantity <= 0)
            """)
    long countProductsOutOfStock(@Param("storeId") UUID storeId);

    @Query("""
            SELECT COUNT(s) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.createdAt >= :from AND s.createdAt < :to
              AND (:status IS NULL OR s.status = :status)
            """)
    long countOrdersInRange(@Param("storeId") UUID storeId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("status") OrderStatus status);

    @Query("""
            SELECT s.status, COUNT(s) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.createdAt >= :from AND s.createdAt < :to
            GROUP BY s.status
            """)
    java.util.List<Object[]> countGroupByStatus(@Param("storeId") UUID storeId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query("""
            SELECT FUNCTION('DATE', s.createdAt) as d, COUNT(s) FROM StoreOrder s
            WHERE s.store.storeId = :storeId AND s.createdAt >= :from AND s.createdAt < :to
            GROUP BY FUNCTION('DATE', s.createdAt)
            ORDER BY d ASC
            """)
    java.util.List<Object[]> countByDay(@Param("storeId") UUID storeId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(s.grandTotal),0) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.status IN (org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED, org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS)
              AND s.createdAt >= :from AND s.createdAt < :to
            """)
    BigDecimal sumRevenueRange(@Param("storeId") UUID storeId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(i.costPrice * i.quantity),0) FROM StoreOrderItem i
            WHERE i.storeOrder.store.storeId = :storeId
              AND i.storeOrder.status IN (org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED, org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS)
              AND i.storeOrder.createdAt >= :from AND i.storeOrder.createdAt < :to
            """)
    BigDecimal sumCostRange(@Param("storeId") UUID storeId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(s.platformFeeAmount),0) FROM StoreOrder s
            WHERE s.store.storeId = :storeId
              AND s.status IN (org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED, org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS)
              AND s.createdAt >= :from AND s.createdAt < :to
            """)
    BigDecimal sumPlatformFeeRange(@Param("storeId") UUID storeId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    // ============================================================
    // 📊 PRODUCT ANALYTICS QUERIES
    // ============================================================

    // Top sản phẩm bán chạy
    @Query("""
            SELECT i.refId as productId, SUM(i.quantity) as totalSold, SUM(i.lineTotal) as totalRevenue
            FROM StoreOrderItem i
            WHERE i.storeOrder.store.storeId = :storeId
              AND i.storeOrder.status IN (org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED, org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS)
              AND i.type = 'PRODUCT'
              AND (:fromDate IS NULL OR i.storeOrder.createdAt >= :fromDate)
              AND (:toDate IS NULL OR i.storeOrder.createdAt < :toDate)
            GROUP BY i.refId
            ORDER BY SUM(i.quantity) DESC
            """)
    java.util.List<Object[]> findTopSellingProducts(@Param("storeId") UUID storeId,
                                                    @Param("fromDate") LocalDateTime fromDate,
                                                    @Param("toDate") LocalDateTime toDate,
                                                    org.springframework.data.domain.Pageable pageable);

    // Đếm số đơn hàng chứa sản phẩm (để tính conversion rate)
    @Query("""
            SELECT i.refId, COUNT(DISTINCT i.storeOrder.id)
            FROM StoreOrderItem i
            WHERE i.storeOrder.store.storeId = :storeId
              AND i.storeOrder.status IN (org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED, org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS)
              AND i.type = 'PRODUCT'
            GROUP BY i.refId
            """)
    java.util.List<Object[]> countOrdersByProduct(@Param("storeId") UUID storeId);
}
