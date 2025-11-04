package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID> {
    List<DeliveryProof> findByStoreOrder_Id(UUID storeOrderId);
}
