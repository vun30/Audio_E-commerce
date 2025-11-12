package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.WarrantyReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarrantyReviewRepository extends JpaRepository<WarrantyReview, UUID> {}
