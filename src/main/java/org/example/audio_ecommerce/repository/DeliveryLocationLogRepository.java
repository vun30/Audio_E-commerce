package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.DeliveryLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryLocationLogRepository extends JpaRepository<DeliveryLocationLog, UUID> {
    List<DeliveryLocationLog> findByStoreOrder_IdOrderByLoggedAtAsc(UUID storeOrderId);
}
