package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {
    boolean existsByName(String name);
}
