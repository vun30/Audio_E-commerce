package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByExternalOrderCode(String externalOrderCode);
    Page<CustomerOrder> findByCustomer_Id(UUID customerId, Pageable pageable);
    Page<CustomerOrder> findByCustomer_IdAndOrderCodeContainingIgnoreCase(
            UUID customerId,
            String orderCode,
            Pageable pageable
    );
    Page<CustomerOrder> findByCustomer_IdAndStatus(UUID customerId,
                                                   OrderStatus status,
                                                   Pageable pageable);
}

