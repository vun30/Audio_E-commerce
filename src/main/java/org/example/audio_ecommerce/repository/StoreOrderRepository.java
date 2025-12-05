package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, UUID> {

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
}
