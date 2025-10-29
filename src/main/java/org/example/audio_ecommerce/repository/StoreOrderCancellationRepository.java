package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreOrderCancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreOrderCancellationRepository extends JpaRepository<StoreOrderCancellationRequest, UUID> {
    List<StoreOrderCancellationRequest> findAllByStoreOrder_Id(UUID storeOrderId);
}
