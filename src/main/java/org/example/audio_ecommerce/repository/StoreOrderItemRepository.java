package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StoreOrderItemRepository extends JpaRepository<StoreOrderItem, UUID> {

    // Lấy items theo storeOrderId
    List<StoreOrderItem> findByStoreOrder_Id(UUID storeOrderId);

    // Lấy items theo storeId + storeOrderId (bảo vệ: order phải thuộc store)
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.id = :storeOrderId
          and i.storeOrder.store.storeId = :storeId
        """)
    List<StoreOrderItem> findItemsOfStoreOrder(UUID storeId, UUID storeOrderId);

    List<StoreOrderItem> findByEligibleForPayoutFalseAndIsPayoutFalse();

    List<StoreOrderItem> findAllByDeliveredAtIsNullAndStoreOrder_DeliveredAtIsNotNull();

    @Query("""
        SELECT i FROM StoreOrderItem i
        WHERE i.storeOrder.store.storeId = :shopId
        AND i.eligibleForPayout = true
        AND i.isPayout = false
    """)
    List<StoreOrderItem> findEligibleForPayout(UUID shopId);

    // ✔ ĐÃ SỬA: dùng Store_StoreId thay vì Store_Id
    List<StoreOrderItem> findAllByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(UUID storeId);

    // ✔ ĐÃ SỬA: dùng Store_StoreId thay vì Store_Id
    boolean existsByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(UUID storeId);

    List<StoreOrderItem> findAllByStoreOrder_ShippingFeeRealIsNotNull();

    List<StoreOrderItem> findAllByStoreOrder_Store_StoreId(UUID storeId);

    // Bucket: ESTIMATED – tất cả item chưa payout (không cần quá detail điều kiện, bạn có thể refine sau)
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and (i.isReturned = false or i.isReturned is null)
          and (i.isPayout = false or i.isPayout is null)
        """)
    Page<StoreOrderItem> findEstimatedItems(@Param("storeId") UUID storeId,
                                            Pageable pageable);

    // Bucket: PENDING – item chưa payout & chưa eligible_for_payout (tiền còn bị giữ)
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and (i.isReturned = false or i.isReturned is null)
          and (i.eligibleForPayout = false or i.eligibleForPayout is null)
          and (i.isPayout = false or i.isPayout is null)
        """)
    Page<StoreOrderItem> findPendingItems(@Param("storeId") UUID storeId,
                                          Pageable pageable);

    // Bucket: DONE – item đã payout xong
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and i.isPayout = true
        """)
    Page<StoreOrderItem> findDoneItems(@Param("storeId") UUID storeId,
                                       Pageable pageable);

    // ..............................

    @Query("""
           select i
           from StoreOrderItem i
           join i.storeOrder so
           join so.store s
           where (:storeId is null or s.storeId = :storeId)
             and so.status = org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
             and i.isReturned = false
             and i.deliveredAt is not null
             and (:from is null or i.deliveredAt >= :from)
             and (:to   is null or i.deliveredAt <  :to)
           """)
    List<StoreOrderItem> findPlatformWalletItems(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ====== Dùng cho DRILL-DOWN /items ======

    // 1) WAITING_SEND_STORE: payoutProcessed = false, eligibleForPayout = false
    @Query("""
           select i
           from StoreOrderItem i
           join i.storeOrder so
           join so.store s
           where (:storeId is null or s.storeId = :storeId)
             and so.status = org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
             and i.isReturned = false
             and i.deliveredAt is not null
             and i.payoutProcessed = false
             and i.eligibleForPayout = false
             and (:from is null or i.deliveredAt >= :from)
             and (:to   is null or i.deliveredAt <  :to)
           """)
    Page<StoreOrderItem> findWaitingSendStoreItems(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 2) FEE_WILL_COLLECT: payoutProcessed = false
    @Query("""
           select i
           from StoreOrderItem i
           join i.storeOrder so
           join so.store s
           where (:storeId is null or s.storeId = :storeId)
             and so.status = org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
             and i.isReturned = false
             and i.deliveredAt is not null
             and i.payoutProcessed = false
             and (:from is null or i.deliveredAt >= :from)
             and (:to   is null or i.deliveredAt <  :to)
           """)
    Page<StoreOrderItem> findFeeWillCollectItems(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 3) FEE_COLLECTED: payoutProcessed = true
    @Query("""
           select i
           from StoreOrderItem i
           join i.storeOrder so
           join so.store s
           where (:storeId is null or s.storeId = :storeId)
             and so.status = org.example.audio_ecommerce.entity.Enum.OrderStatus.DELIVERY_SUCCESS
             and i.isReturned = false
             and i.deliveredAt is not null
             and i.payoutProcessed = true
             and (:from is null or i.deliveredAt >= :from)
             and (:to   is null or i.deliveredAt <  :to)
           """)
    Page<StoreOrderItem> findFeeCollectedItems(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

}
