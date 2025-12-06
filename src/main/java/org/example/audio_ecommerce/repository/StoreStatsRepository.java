package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoreStatsRepository extends JpaRepository<StoreOrder, UUID> {

    // ========================================================
    // 1) DELIVERED ORDERS — TRỌN ĐỜI
    // ========================================================
    @Query("""
        SELECT o
        FROM StoreOrder o
        WHERE o.store.storeId = :storeId
          AND o.status IN (
              org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED,
              org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
          )
    """)
    List<StoreOrder> findAllDeliveredOrders(@Param("storeId") UUID storeId);


    // ========================================================
    // 2) DELIVERED ORDERS — THEO KHOẢNG NGÀY
    // ========================================================
    @Query("""
        SELECT o
        FROM StoreOrder o
        WHERE o.store.storeId = :storeId
          AND o.status IN (
              org.example.audio_ecommerce.entity.Enum.OrderStatus.COMPLETED,
              org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
          )
          AND o.deliveredAt >= :start
          AND o.deliveredAt < :end
    """)
    List<StoreOrder> findDeliveredOrdersBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    // ========================================================
    // 3) LẤY STORE ORDER ITEM THEO DANH SÁCH ORDER ID
    // ========================================================
    @Query("""
        SELECT i
        FROM StoreOrderItem i
        WHERE i.storeOrder.id IN :orderIds
    """)
    List<StoreOrderItem> findItemsByOrderIds(@Param("orderIds") List<UUID> orderIds);


    // ========================================================
    // 4) RETURN REQUEST — TRỌN ĐỜI
    // ========================================================
    @Query("""
        SELECT r
        FROM ReturnRequest r
        WHERE r.shopId = :storeId
    """)
    List<ReturnRequest> findAllReturnRequests(@Param("storeId") UUID storeId);


    // ========================================================
    // 5) RETURN REQUEST — THEO KHOẢNG NGÀY
    // ========================================================
    @Query("""
        SELECT r
        FROM ReturnRequest r
        WHERE r.shopId = :storeId
          AND r.createdAt >= :start
          AND r.createdAt < :end
    """)
    List<ReturnRequest> findReturnRequestsBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    // ========================================================
    // 6) COUNT RETURN SUCCESS TRONG KHOẢNG NGÀY
    // ========================================================
    @Query("""
        SELECT COUNT(r)
        FROM ReturnRequest r
        WHERE r.shopId = :storeId
          AND r.status IN (
              org.example.audio_ecommerce.entity.Enum.ReturnStatus.RETURN_DONE,
              org.example.audio_ecommerce.entity.Enum.ReturnStatus.DISPUTE_RESOLVED_CUSTOMER,
              org.example.audio_ecommerce.entity.Enum.ReturnStatus.REFUNDED
          )
          AND r.createdAt >= :start
          AND r.createdAt < :end
    """)
    long countReturnSuccessBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    // ========================================================
    // 7) SHIPPING FEE DIFFERENCE (PHÍ SHIP CHÊNH LỆCH)
    // ========================================================
    @Query("""
        SELECT COALESCE(SUM(o.shippingFeeForStore), 0)
        FROM StoreOrder o
        WHERE o.store.storeId = :storeId
          AND o.shippingFeeForStore IS NOT NULL
          AND o.shippingFeeForStore > 0
          AND o.deliveredAt >= :start
          AND o.deliveredAt < :end
    """)
    BigDecimal sumShippingDifferenceBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    // ========================================================
    // 8) RETURN SHIPPING FEE (PHÍ SHIP RETURN GHN)
    // ========================================================
    @Query("""
        SELECT COALESCE(SUM(r.shippingFee), 0)
        FROM ReturnShippingFee r
        WHERE r.storeId = :storeId
          AND r.createdAt >= :start
          AND r.createdAt < :end
    """)
    BigDecimal sumReturnShippingFeeBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    // ========================================================
    // 9) SHIPPING COLLECTED FEE (PHÍ GHN ĐÃ THU)
    // loại trừ UNPAID, CONFIRMED, AWAITING_SHIPMENT
    // ========================================================
    @Query("""
        SELECT COALESCE(SUM(o.actualShippingFee), 0)
        FROM StoreOrder o
        WHERE o.store.storeId = :storeId
          AND o.status NOT IN (
              org.example.audio_ecommerce.entity.Enum.OrderStatus.UNPAID,
              org.example.audio_ecommerce.entity.Enum.OrderStatus.CONFIRMED,
              org.example.audio_ecommerce.entity.Enum.OrderStatus.AWAITING_SHIPMENT
          )
          AND o.deliveredAt >= :start
          AND o.deliveredAt < :end
    """)
    BigDecimal sumActualShippingFeeBetween(
            @Param("storeId") UUID storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

}
