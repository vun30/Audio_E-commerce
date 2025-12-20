package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, UUID>, JpaSpecificationExecutor<StoreOrder> {

    List<StoreOrder> findAllByCustomerOrder_Id(UUID customerOrderId);

    Page<StoreOrder> findByStore_StoreId(UUID storeId, Pageable pageable);

    List<StoreOrder> findAllByStore_StoreId(UUID customerOrderId);

    Page<StoreOrder> findByStore_StoreIdAndOrderCode(UUID storeId, String orderCode, Pageable pageable);

    Page<StoreOrder> findByStore_StoreIdAndOrderCodeContainingIgnoreCase(UUID storeId, String keyword, Pageable pageable);

    List<StoreOrder> findAllByCustomerOrder(CustomerOrder customerOrder);

    // ❗ SHIPPING FEE CHƯA TRẢ (paid_by_shop = false OR NULL)
    @Query("""
        SELECT o FROM StoreOrder o
        WHERE o.store.storeId = :storeId
        AND o.shippingFeeForStore IS NOT NULL
        AND o.shippingFeeForStore <> 0
        AND (o.paidByShop = false OR o.paidByShop IS NULL)
    """)
    List<StoreOrder> findPendingShippingOrders(UUID storeId);


    // API cũ để tương thích, nhưng không dùng nữa
    List<StoreOrder> findAllByStore_StoreIdAndPaidByShopFalse(UUID storeId);

    boolean existsByStore_StoreIdAndPaidByShopFalse(UUID storeId);

    // 🔹 Thêm default method searchStoreOrders dùng Specification
    default Page<StoreOrder> searchStoreOrders(
            UUID storeId,
            String orderCodeKeyword,
            OrderStatus status,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            Pageable pageable
    ) {
        return findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            // filter theo storeId (bắt buộc)
            predicates.add(cb.equal(root.get("store").get("storeId"), storeId));

            // filter theo status (optional)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // filter theo createdAt from/to (optional)
            if (fromDateTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
            }
            if (toDateTime != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toDateTime));
            }

            // filter theo orderCodeKeyword (optional)
            if (orderCodeKeyword != null && !orderCodeKeyword.isBlank()) {
                String pattern = "%" + orderCodeKeyword.toLowerCase() + "%";
                predicates.add(
                        cb.like(cb.lower(root.get("orderCode")), pattern)
                );
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    @Query("select so from StoreOrder so left join fetch so.items i where so.paymentMethod = :pm and so.deliveredAt is null")
    List<StoreOrder> findUnDeliveredByPaymentMethodFetchItems(@Param("pm") PaymentMethod pm);

    @Query("select so from StoreOrder so left join fetch so.items i where so.deliveredAt between :from and :to")
    List<StoreOrder> findDeliveredBetweenFetchItems(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select so from StoreOrder so left join fetch so.items i where so.store.storeId = :storeId and so.deliveredAt between :from and :to")
    List<StoreOrder> findDeliveredBetweenByStoreFetchItems(@Param("storeId") UUID storeId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select so from StoreOrder so left join fetch so.items i where so.deliveredAt = :date")
    List<StoreOrder> findDeliveredAtFetchItems(@Param("date") LocalDateTime date);

    // fallback: fetch all storeOrders with items (careful perf)
    @Query("select distinct so from StoreOrder so left join fetch so.items")
    List<StoreOrder> findAllWithItemsFetch();

    List<StoreOrder> findByStatusAndStoreScoredFalse(OrderStatus status);
    @Query("""
    select o from StoreOrder o
    where o.paidByShop = false
      and o.shippingFeeReal is not null
      and o.shippingFeeReal > 0
""")
    List<StoreOrder> findOrdersForDebtCron();

    @Query("""
    select o.store.storeId, coalesce(sum(o.totalDebtOrder), 0)
    from StoreOrder o
    where o.paidByShop = false
      and o.totalDebtOrder > 0
    group by o.store.storeId
""")
    List<Object[]> sumDebtFromOrdersByStore();

    List<StoreOrder> findByStore_StoreIdAndPaidByShopFalse(UUID storeId);

    List<StoreOrder> findByStore_StoreIdAndPaidByShopFalseAndCreatedAtBetween(
            UUID storeId, LocalDateTime from, LocalDateTime to);

    @Query("""
   select o from StoreOrder o
   where o.store.storeId = :storeId
     and o.paidByShop = false
     and (o.deliveredAt is not null or o.returnChargeApplied = true)
""")
    List<StoreOrder> findUnpaidFinalOrdersOfStore(@Param("storeId") UUID storeId);

    List<StoreOrder> findByStatusAndReturnChargeApplied(
            OrderStatus status,
            Boolean returnChargeApplied
    );

    @Query("""
    select distinct s
    from Store s
    join fetch s.wallet w
    where s.status in :statuses
""")
    List<Store> findStoresWithWalletByStatuses(@Param("statuses") List<StoreStatus> statuses);

    @Query("""
        select count(o)
        from StoreOrder o
        where o.store.storeId = :storeId
          and o.deliveredAt is not null
          and o.deliveredAt >= :from
          and o.deliveredAt <= :to
    """)
    long countDeliveredOrders(
            @Param("storeId") UUID storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(value = """
        select
            year(o.delivered_at) as y,
            month(o.delivered_at) as m,
            count(*) as deliveredOrders
        from store_order o
        where o.store_id = :storeId
          and o.delivered_at is not null
          and year(o.delivered_at) = :year
        group by year(o.delivered_at), month(o.delivered_at)
        order by y asc, m asc
    """, nativeQuery = true)
    List<Object[]> deliveredOrdersByMonth(
            @Param("storeId") UUID storeId,
            @Param("year") int year
    );

    @Query(value = """
        select
            year(o.delivered_at) as y,
            count(*) as deliveredOrders
        from store_order o
        where o.store_id = :storeId
          and o.delivered_at is not null
          and year(o.delivered_at) between :fromYear and :toYear
        group by year(o.delivered_at)
        order by y asc
    """, nativeQuery = true)
    List<Object[]> deliveredOrdersByYear(
            @Param("storeId") UUID storeId,
            @Param("fromYear") int fromYear,
            @Param("toYear") int toYear
    );


}
