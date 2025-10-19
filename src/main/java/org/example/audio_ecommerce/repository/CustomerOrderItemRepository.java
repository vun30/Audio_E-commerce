package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, UUID> {
}

