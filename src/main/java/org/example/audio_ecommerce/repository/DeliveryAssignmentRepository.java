package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {
    Optional<DeliveryAssignment> findByStoreOrder_Id(UUID storeOrderId);
}
