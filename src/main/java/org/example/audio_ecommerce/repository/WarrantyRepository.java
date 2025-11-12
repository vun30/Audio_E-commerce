package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Warranty;
import org.example.audio_ecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarrantyRepository extends JpaRepository<Warranty, UUID> {
    Optional<Warranty> findBySerialNumber(String serialNumber);
    List<Warranty> findByCustomer(Customer customer);
    List<Warranty> findByEndDateBeforeAndStatus(LocalDate date, org.example.audio_ecommerce.entity.Enum.WarrantyStatus status);
    List<Warranty> findByStoreOrderItemId(UUID storeOrderItemId);
}
