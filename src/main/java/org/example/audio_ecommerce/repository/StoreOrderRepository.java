package org.example.audio_ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, UUID> {
    List<StoreOrder> findAllByCustomerOrder_Id(UUID customerOrderId);
    Page<StoreOrder> findByStore_StoreId(UUID storeId, Pageable pageable);
    List<StoreOrder> findAllByStore_StoreId(UUID customerOrderId);
}

    