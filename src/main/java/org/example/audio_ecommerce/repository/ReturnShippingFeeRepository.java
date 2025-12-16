package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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



}
