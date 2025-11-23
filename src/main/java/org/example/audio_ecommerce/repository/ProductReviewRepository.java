package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ProductReview;
import org.example.audio_ecommerce.entity.Enum.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    Page<ProductReview> findByStore_StoreIdAndStatus(UUID storeId, ReviewStatus status, Pageable pageable);

    Optional<ProductReview> findByOrderItem_IdAndCustomer_Id(UUID orderItemId, UUID customerId);

    Page<ProductReview> findByProduct_ProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByProduct_ProductIdAndStore_StoreIdAndStatus(
            UUID productId, UUID storeId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByCustomer_Id(UUID customerId, Pageable pageable);

    Optional<ProductReview> findByProduct_ProductIdAndCustomer_IdAndStatus(
            UUID productId, UUID customerId, ReviewStatus status);
}
