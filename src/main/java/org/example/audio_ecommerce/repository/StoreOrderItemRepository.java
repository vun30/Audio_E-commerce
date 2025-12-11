package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

     @Query("""
        SELECT COUNT(i)
        FROM StoreOrderItem i
        WHERE i.refId = :productId AND i.type = 'PRODUCT'
    """)
    int countOrdersByProduct(UUID productId);

    @Query("""
        SELECT COUNT(i)
        FROM StoreOrderItem i
        WHERE i.variantId = :variantId
    """)
    int countOrdersByVariant(UUID variantId);

}
