package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.repository.projection.ReturnShipFeeAgg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnShippingFeeRepository extends JpaRepository<ReturnShippingFee, UUID> {
    Optional<ReturnShippingFee> findByReturnRequestId(UUID returnRequestId);
    List<ReturnShippingFee> findByGhnOrderCodeAndPickedFalse(String ghnOrderCode);

    @Query("""
        SELECT r FROM ReturnShippingFee r
        WHERE r.storeId = :shopId
        AND r.paidByShop = false
    """)
    List<ReturnShippingFee> findUnpaidByShop(UUID shopId);

     List<ReturnShippingFee> findAllByStoreIdAndPaidByShopFalse(UUID storeId);

     boolean existsByStoreIdAndPaidByShopFalse(UUID storeId);
    Optional<ReturnShippingFee> findByGhnOrderCode(String ghnOrderCode);

    @Query("""
    select r.storeId, coalesce(sum(r.shippingFee), 0)
    from ReturnShippingFee r
    where r.paidByShop = false
      and r.payer = 'SHOP'
      and r.shippingFee > 0
    group by r.storeId
""")
    List<Object[]> sumDebtFromReturnFeesByStore();

    List<ReturnShippingFee> findByStoreIdAndPayerAndPaidByShopFalse(UUID storeId, String payer);

    List<ReturnShippingFee> findByStoreIdAndPayerAndPaidByShopFalseAndCreatedAtBetween(
            UUID storeId, String payer, LocalDateTime from, LocalDateTime to);

    @Query("""
   select f from ReturnShippingFee f
   where f.storeId = :storeId
     and f.payer = 'SHOP'
     and f.paidByShop = false
""")
    List<ReturnShippingFee> findUnpaidShopReturnFees(@Param("storeId") UUID storeId);

    List<ReturnShippingFee> findByStoreIdAndPayerIgnoreCaseAndPaidByShopFalse(UUID storeId, String payer);

    @Query("""
        select coalesce(sum(r.chargedToShop), 0)
        from ReturnShippingFee r
        where r.payer = 'SHOP'
          and r.paidByShop = false
          and (:from is null or r.createdAt >= :from)
          and (:to   is null or r.createdAt <= :to)
    """)
    BigDecimal sumUnpaidReturnByRange(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("""
        select coalesce(sum(r.chargedToShop), 0)
        from ReturnShippingFee r
        where r.payer = 'SHOP'
          and r.paidByShop = true
          and (:from is null or r.createdAt >= :from)
          and (:to   is null or r.createdAt <= :to)
    """)
    BigDecimal sumPaidReturnByRange(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);


    @Query("""
    select coalesce(sum(r.chargedToShop), 0)
    from ReturnShippingFee r
    where r.payer = 'SHOP'
      and r.ghnDebtFinalized = true
      and (:from is null or r.createdAt >= :from)
      and (:to is null or r.createdAt <= :to)
""")
    BigDecimal sumReturnFinalizedByRange(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("""
    select coalesce(sum(r.chargedToShop), 0)
    from ReturnShippingFee r
    where r.payer = 'SHOP'
      and r.ghnDebtFinalized = false
      and (:from is null or r.createdAt >= :from)
      and (:to is null or r.createdAt <= :to)
""")
    BigDecimal sumReturnNotFinalizedByRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);


    @Query("""
    select coalesce(sum(r.shippingFee), 0)
    from ReturnShippingFee r
    where r.storeId = :storeId
      and r.paidByShop = false
      and r.payer = 'SHOP'
      and r.shippingFee > 0
""")
    BigDecimal sumDebtReturnFeesByStoreId(@Param("storeId") UUID storeId);

    @Query("""
    select coalesce(sum(r.shippingFee), 0)
    from ReturnShippingFee r
    where (:from is null or r.createdAt >= :from)
      and (:toExclusive is null or r.createdAt < :toExclusive)
    """)
    BigDecimal sumReturnShipFee(@Param("from") LocalDateTime from,
                                @Param("toExclusive") LocalDateTime toExclusive);


    @Query(value = """
        SELECT
          COALESCE(SUM(
            CASE
              WHEN rsf.paid_by_shop = 1
              THEN rsf.shipping_fee
              ELSE 0
            END
          ),0) AS paid,

          COALESCE(SUM(
            CASE
              WHEN rsf.paid_by_shop = 0 OR rsf.paid_by_shop IS NULL
              THEN rsf.shipping_fee
              ELSE 0
            END
          ),0) AS outstanding
        FROM return_shipping_fees rsf
        WHERE (:from IS NULL OR rsf.created_at >= :from)
          AND (:toExclusive IS NULL OR rsf.created_at < :toExclusive)
        """, nativeQuery = true)
    ReturnShipFeeAgg aggReturnShipFee(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}

