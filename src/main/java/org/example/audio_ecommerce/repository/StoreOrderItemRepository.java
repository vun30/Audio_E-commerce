package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
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
    List<StoreOrderItem> findItemsOfStoreOrder(@Param("storeId") UUID storeId,
                                               @Param("storeOrderId") UUID storeOrderId);

    List<StoreOrderItem> findByEligibleForPayoutFalseAndIsPayoutFalse();

    List<StoreOrderItem> findAllByDeliveredAtIsNullAndStoreOrder_DeliveredAtIsNotNull();

    @Query("""
        SELECT i FROM StoreOrderItem i
        WHERE i.storeOrder.store.storeId = :shopId
          AND i.eligibleForPayout = true
          AND i.isPayout = false
    """)
    List<StoreOrderItem> findEligibleForPayout(@Param("shopId") UUID shopId);

    // ✔ ĐÃ SỬA: dùng Store_StoreId thay vì Store_Id
    List<StoreOrderItem> findAllByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(UUID storeId);

    // ✔ ĐÃ SỬA: dùng Store_StoreId thay vì Store_Id
    boolean existsByStoreOrder_Store_StoreIdAndEligibleForPayoutTrueAndIsPayoutFalse(UUID storeId);

    List<StoreOrderItem> findAllByStoreOrder_ShippingFeeRealIsNotNull();

    @Query("""
        SELECT COUNT(i)
        FROM StoreOrderItem i
        WHERE i.refId = :productId AND i.type = 'PRODUCT'
    """)
    int countOrdersByProduct(@Param("productId") UUID productId);

    @Query("""
        SELECT COUNT(i)
        FROM StoreOrderItem i
        WHERE i.variantId = :variantId
    """)
    int countOrdersByVariant(@Param("variantId") UUID variantId);

    List<StoreOrderItem> findAllByStoreOrder_Store_StoreId(UUID storeId);

    // Bucket: ESTIMATED – tất cả item chưa payout
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and (i.isReturned = false or i.isReturned is null)
          and (i.isPayout = false or i.isPayout is null)
    """)
    Page<StoreOrderItem> findEstimatedItems(@Param("storeId") UUID storeId, Pageable pageable);

    // Bucket: PENDING – item chưa payout & chưa eligible_for_payout (tiền còn bị giữ)
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and (i.isReturned = false or i.isReturned is null)
          and (i.eligibleForPayout = false or i.eligibleForPayout is null)
          and (i.isPayout = false or i.isPayout is null)
    """)
    Page<StoreOrderItem> findPendingItems(@Param("storeId") UUID storeId, Pageable pageable);

    // Bucket: DONE – item đã payout xong
    @Query("""
        select i from StoreOrderItem i
        where i.storeOrder.store.storeId = :storeId
          and i.isPayout = true
    """)
    Page<StoreOrderItem> findDoneItems(@Param("storeId") UUID storeId, Pageable pageable);

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

    @Query("""
        select distinct so.customerOrder.id
        from StoreOrderItem i
        join i.storeOrder so
        join so.customerOrder co
        where i.eligibleForPayout = true
          and (i.isPayout is null or i.isPayout = false)
          and (
                (co.deliveredAt is not null and co.deliveredAt <= :cutoffTime)
             or (co.deliveredAt is null and co.createdAt <= :cutoffTime)
          )
    """)
    List<UUID> findEligibleCustomerOrderIdsForPayout(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
        SELECT i FROM StoreOrderItem i
        WHERE (:storeId IS NULL OR i.storeOrder.store.storeId = :storeId)
          AND i.eligibleForPayout = TRUE
          AND i.isPayout = :isPayout
          AND (
                (:isDelivered = TRUE  AND i.deliveredAt IS NOT NULL)
             OR (:isDelivered = FALSE AND i.deliveredAt IS NULL)
          )
          AND i.storeOrder.paymentMethod = :paymentMethod
          AND i.storeOrder.createdAt BETWEEN :fromDate AND :toDate
    """)
    List<StoreOrderItem> findItemsForOverview(
            @Param("storeId") UUID storeId,
            @Param("isDelivered") boolean isDelivered,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("isPayout") boolean isPayout,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // ============================================================
    // 2) PLATFORM FEE (item đã giao nhưng chưa payout)
    // ============================================================
    @Query("""
        SELECT i FROM StoreOrderItem i
        WHERE (:storeId IS NULL OR i.storeOrder.store.storeId = :storeId)
          AND i.platformFeePercentage > 0
          AND i.deliveredAt BETWEEN :fromDate AND :toDate
    """)
    List<StoreOrderItem> findPlatformFeeItems(
            @Param("storeId") UUID storeId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // ============================================================
    // 3) TỔNG TIỀN ĐÃ TRẢ CHO SHOP (item đã payout)
    // ============================================================
    @Query("""
        SELECT i FROM StoreOrderItem i
        WHERE (:storeId IS NULL OR i.storeOrder.store.storeId = :storeId)
          AND i.isPayout = TRUE
          AND i.deliveredAt BETWEEN :fromDate AND :toDate
    """)
    List<StoreOrderItem> findPaidItems(
            @Param("storeId") UUID storeId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // ============================================================
    // ✅ SUM (FIX) - đổi Object[] -> List<Object[]>
    // ============================================================

    // ---------- 1) Pending balance (hold) ----------
    @Query("""
        select
          count(i),
          coalesce(sum(i.finalLineTotal), 0)
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and (i.eligibleForPayout = false or i.eligibleForPayout is null)
          and (i.isPayout = false or i.isPayout is null)
          and (i.isReturned = false or i.isReturned is null)
    """)
    List<Object[]> sumPendingBalance(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ---------- 2) Eligible but not payout => platform fee payable ----------
    @Query("""
        select
          count(i),
          coalesce(sum(i.finalLineTotal), 0),
          coalesce(sum(i.finalLineTotal * (i.platformFeePercentage / 100.0)), 0)
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and i.eligibleForPayout = true
          and (i.isPayout = false or i.isPayout is null)
          and (i.isReturned = false or i.isReturned is null)
    """)
    List<Object[]> sumPlatformFeePayable(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ---------- 3) Payout done => available balance ----------
    @Query("""
        select
          count(i),
          coalesce(sum(i.finalLineTotal), 0),
          coalesce(sum(i.finalLineTotal * (i.platformFeePercentage / 100.0)), 0)
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and i.eligibleForPayout = true
          and i.isPayout = true
          and (i.isReturned = false or i.isReturned is null)
    """)
    List<Object[]> sumAvailableBalance(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ============================================================
    // BREAKDOWN ITEMS (bucket + from/to + paging)
    // ============================================================

    // 1) PENDING (hold)
    @Query("""
        select i
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and (i.eligibleForPayout = false or i.eligibleForPayout is null)
          and (i.isPayout = false or i.isPayout is null)
          and (i.isReturned = false or i.isReturned is null)
    """)
    Page<StoreOrderItem> findPendingBreakdown(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 2) ELIGIBLE_NOT_PAYOUT
    @Query("""
        select i
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and i.eligibleForPayout = true
          and (i.isPayout = false or i.isPayout is null)
          and (i.isReturned = false or i.isReturned is null)
    """)
    Page<StoreOrderItem> findEligibleNotPayoutBreakdown(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 3) PAYOUT_DONE
    @Query("""
        select i
        from StoreOrderItem i
        join i.storeOrder so
        join so.store s
        where s.storeId = :storeId
          and i.deliveredAt is not null
          and i.deliveredAt >= :from and i.deliveredAt <= :to
          and i.eligibleForPayout = true
          and i.isPayout = true
          and (i.isReturned = false or i.isReturned is null)
    """)
    Page<StoreOrderItem> findPayoutDoneBreakdown(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    // StoreOrderItemRepository.java
    @Query("""
    select i
    from StoreOrderItem i
    join i.storeOrder so
    join so.store s
    where s.storeId = :storeId
      and i.deliveredAt is not null
      and i.eligibleForPayout = true
      and (i.isPayout = false or i.isPayout is null)
      and (i.isReturned = false or i.isReturned is null)
""")
    List<StoreOrderItem> findEligibleItemsToAutoPayout(@Param("storeId") UUID storeId);



}
