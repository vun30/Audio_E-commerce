package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductCombo;
import org.example.audio_ecommerce.entity.Enum.ComboCreatorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductComboRepository extends JpaRepository<ProductCombo, UUID> {
    Page<ProductCombo> findByCreatorTypeAndCreatorId(ComboCreatorType creatorType, UUID creatorId, Pageable pageable);
}
