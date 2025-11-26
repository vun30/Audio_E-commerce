package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrderItem;
import org.example.audio_ecommerce.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, UUID> {
    Optional<CustomerOrderItem> findByCustomerOrder_IdAndRefIdAndType(
            UUID customerOrderId, UUID refId, String type);
}

