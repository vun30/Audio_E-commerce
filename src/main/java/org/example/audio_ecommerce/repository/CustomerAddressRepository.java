package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);
    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);
    long countByCustomerId(UUID customerId);
    List<CustomerAddress> findByCustomer_IdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);
}
