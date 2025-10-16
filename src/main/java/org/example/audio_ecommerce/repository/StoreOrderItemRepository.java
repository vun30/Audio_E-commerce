package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StoreOrderItemRepository extends JpaRepository<StoreOrderItem, UUID> {
}

