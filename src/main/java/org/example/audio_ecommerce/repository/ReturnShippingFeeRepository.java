package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
