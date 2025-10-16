package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
}

