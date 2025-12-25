package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.projection.FlatOrderAgg2;
import org.example.audio_ecommerce.service.Projection.FlatDebtOrderRow;
import org.example.audio_ecommerce.service.Projection.FlatOrderAgg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    Optional<StoreOrder> findFirstByCustomerOrder_Id(UUID customerOrderId);

    @Query("""
        select so
        from StoreOrder so
        where so.status = :status
          and so.storeScored = false
    """)
    List<StoreOrder> findDeliverySuccessNotScored(
            @Param("status") OrderStatus status
    );


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

    List<StoreOrder> findByStatusAndSellCountUpdatedFalse(OrderStatus status);
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

    // ===============================
// ✅ FLAT / GHN - SUMMARY BY ORDER RULES
// ===============================

    /**
     * 1) Flat nợ GHN (shippingFeeReal nếu delivered, shippingFeeReal*1.5 nếu RETURNING chưa delivered)
     * Không tính các status: UNPAID, CONFIRMED, AWAITING_SHIPMENT, CANCELLED, PENDING
     */
    @Query(value = """
    SELECT COALESCE(SUM(
        CASE
            WHEN (:from IS NULL OR so.created_at >= :from)
             AND (:to   IS NULL OR so.created_at <= :to)
            THEN
                CASE
                    WHEN so.delivered_at IS NOT NULL
                        THEN COALESCE(so.shipping_fee_real, 0)
                    WHEN so.delivered_at IS NULL AND so.status = 'RETURNING'
                        THEN COALESCE(so.shipping_fee_real, 0) * 1.5
                    ELSE 0
                END
            ELSE 0
        END
    ), 0)
    FROM store_order so
    WHERE so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
""", nativeQuery = true)
    BigDecimal sumFlatDebtToGHN(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /**
     * 2) Store còn nợ flat (paid_by_shop = 0/false)
     */
    @Query(value = """
    SELECT COALESCE(SUM(
        CASE
            WHEN (:from IS NULL OR so.created_at >= :from)
             AND (:to   IS NULL OR so.created_at <= :to)
            THEN
                CASE
                    WHEN so.delivered_at IS NOT NULL
                        THEN COALESCE(so.shipping_fee_real, 0)
                    WHEN so.delivered_at IS NULL AND so.status = 'RETURNING'
                        THEN COALESCE(so.shipping_fee_real, 0) * 1.5
                    ELSE 0
                END
            ELSE 0
        END
    ), 0)
    FROM store_order so
    WHERE (so.paid_by_shop = 0 OR so.paid_by_shop IS NULL)
      AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
""", nativeQuery = true)
    BigDecimal sumStoreDebtOutstandingToFlat(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /**
     * 3) Store đã trả flat (paid_by_shop = 1/true)
     */
    @Query(value = """
    SELECT COALESCE(SUM(
        CASE
            WHEN (:from IS NULL OR so.created_at >= :from)
             AND (:to   IS NULL OR so.created_at <= :to)
            THEN
                CASE
                    WHEN so.delivered_at IS NOT NULL
                        THEN COALESCE(so.shipping_fee_real, 0)
                    WHEN so.delivered_at IS NULL AND so.status = 'RETURNING'
                        THEN COALESCE(so.shipping_fee_real, 0) * 1.5
                    ELSE 0
                END
            ELSE 0
        END
    ), 0)
    FROM store_order so
    WHERE so.paid_by_shop = 1
      AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
""", nativeQuery = true)
    BigDecimal sumStoreDebtPaidToFlat(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /**
     * 4) Tiền ship khách đã trả (chỉ tính khi deliveredAt != null)
     * Theo bạn: nếu chưa delivered thì chưa tính.
     */
    @Query(value = """
    SELECT COALESCE(SUM(
        CASE
            WHEN (:from IS NULL OR so.created_at >= :from)
             AND (:to   IS NULL OR so.created_at <= :to)
            THEN
                CASE
                    WHEN so.delivered_at IS NOT NULL
                        THEN COALESCE(so.shipping_fee, 0)
                    ELSE 0
                END
            ELSE 0
        END
    ), 0)
    FROM store_order so
""", nativeQuery = true)
    BigDecimal sumCustomerShipPaidDelivered(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query(value = """
    SELECT COALESCE(SUM(
        CASE
            WHEN (:from IS NULL OR so.created_at >= :from)
             AND (:to   IS NULL OR so.created_at <= :to)
            THEN
                CASE
                    WHEN so.delivered_at IS NOT NULL
                        THEN COALESCE(so.shipping_fee_real, 0)
                    WHEN so.delivered_at IS NULL AND so.status = 'RETURNING'
                        THEN COALESCE(so.shipping_fee_real, 0) * 1.5
                    ELSE 0
                END
            ELSE 0
        END
    ), 0)
    FROM store_order so
    WHERE so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
""", nativeQuery = true)
    BigDecimal sumStoreDebtTotalToFlat(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);





    @Query(value = """
    SELECT
        so.status            AS status,
        so.created_at        AS createdAt,
        so.delivered_at      AS deliveredAt,
        so.shipping_fee_real AS shippingFeeReal,
        so.shipping_fee      AS shippingFee,
        so.paid_by_shop      AS paidByShop
    FROM store_order so
    WHERE (:from IS NULL OR so.created_at >= :from)
      AND (:to   IS NULL OR so.created_at <= :to)

      -- chỉ lấy order có phát sinh nợ ship
      AND so.shipping_fee_real IS NOT NULL
      AND so.shipping_fee_real > 0

      -- rule tính nợ
      AND (
            so.delivered_at IS NOT NULL
         OR (so.delivered_at IS NULL AND so.status = 'RETURNING')
      )

      -- loại bỏ trạng thái không tính nợ
      AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
""", nativeQuery = true)
    List<FlatDebtOrderRow> findFlatDebtRows(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(value = """
SELECT
  -- (1) Flat nợ GHN theo rule (delivered => shipReal, returning not delivered => shipReal*1.5)
  COALESCE(SUM(
    CASE
      WHEN so.shipping_fee_real IS NOT NULL
       AND so.shipping_fee_real > 0
       AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','EXCEPTION','CANCELLED')
       AND (
            so.delivered_at IS NOT NULL
         OR (so.delivered_at IS NULL AND so.status = 'RETURNING')
       )
      THEN
        CASE
          WHEN so.delivered_at IS NOT NULL THEN COALESCE(so.shipping_fee_real,0)
          ELSE COALESCE(so.shipping_fee_real,0) * 1.5
        END
      ELSE 0
    END
  ),0) AS flatDebtShipToGhn,

  -- (2) Cus trả ship: chỉ delivered
  COALESCE(SUM(
    CASE
      WHEN so.delivered_at IS NOT NULL
       AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','EXCEPTION','CANCELLED')
      THEN COALESCE(so.shipping_fee,0)
      ELSE 0
    END
  ),0) AS customerShipPaid,

  -- (3) Store đã trả nợ cho flat: paid_by_shop = true (tính theo cùng rule debt)
  COALESCE(SUM(
    CASE
      WHEN so.paid_by_shop = 1
       AND so.shipping_fee_real IS NOT NULL
       AND so.shipping_fee_real > 0
       AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','EXCEPTION','CANCELLED')
       AND (
            so.delivered_at IS NOT NULL
         OR (so.delivered_at IS NULL AND so.status = 'RETURNING')
       )
      THEN
        CASE
          WHEN so.delivered_at IS NOT NULL THEN COALESCE(so.shipping_fee_real,0)
          ELSE COALESCE(so.shipping_fee_real,0) * 1.5
        END
      ELSE 0
    END
  ),0) AS storeDebtPaidToFlat

FROM store_order so
WHERE (:from IS NULL OR so.created_at >= :from)
  AND (:toExclusive IS NULL OR so.created_at < :toExclusive)
""", nativeQuery = true)
    FlatOrderAgg2 aggFlatOverview(@Param("from") LocalDateTime from,
                                  @Param("toExclusive") LocalDateTime toExclusive);



    @Query("""
    select coalesce(sum(o.totalDebtOrder), 0)
    from StoreOrder o
    where o.store.storeId = :storeId
      and (o.paidByShop = false or o.paidByShop is null)
""")
    BigDecimal sumDebtOrdersByStoreId(@Param("storeId") UUID storeId);

    @Query("""
        select o
        from StoreOrder o
        where o.store.storeId = :storeId
          and o.paidByShop = false
          and o.totalDebtOrder > 0
          and o.status in :endStatuses
        order by o.createdAt desc
    """)
    List<StoreOrder> findUnpaidDebtEndOrdersByStore(
            @Param("storeId") UUID storeId,
            @Param("endStatuses") List<OrderStatus> endStatuses
    );

    List<StoreOrder> findByStatusAndPaymentMethodAndCodCollectedFalse(
            OrderStatus status,
            PaymentMethod paymentMethod
    );

    // 12h không confirm (status còn PENDING)
    List<StoreOrder> findAllByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time);

    // 24h sau confirm mà chưa bàn giao GHN
    @Query("""
        select so
        from StoreOrder so
        where so.status = :status
          and so.confirmedAt is not null
          and so.confirmedAt < :deadline
          and (so.ghnHandoverAt is null)
          and so.autoCancelApplied = false
    """)
    List<StoreOrder> findNeedAutoCancelAfterConfirm(
            @Param("status") OrderStatus status,
            @Param("deadline") LocalDateTime deadline
    );

}
