package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.LogWarranty;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LogWarrantyRepository extends JpaRepository<LogWarranty, UUID> {
    List<LogWarranty> findByWarrantyId(UUID warrantyId);
    List<LogWarranty> findByWarrantyIdAndStatus(UUID warrantyId, WarrantyLogStatus status);
}
