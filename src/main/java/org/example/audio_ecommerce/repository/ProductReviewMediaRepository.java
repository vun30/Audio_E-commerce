package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductReviewMediaRepository extends JpaRepository<ProductReviewMedia, UUID> {
}
