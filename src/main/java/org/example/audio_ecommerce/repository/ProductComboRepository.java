package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductComboRepository extends JpaRepository<ProductCombo, UUID> {
}
