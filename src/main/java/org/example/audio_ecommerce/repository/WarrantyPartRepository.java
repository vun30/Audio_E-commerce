package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.WarrantyPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarrantyPartRepository extends JpaRepository<WarrantyPart, UUID> {}
