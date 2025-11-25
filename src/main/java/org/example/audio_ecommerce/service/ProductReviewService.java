package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ProductReviewCreateRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewReplyRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewUpdateRequest;
import org.example.audio_ecommerce.dto.response.ProductReviewResponse;
import org.example.audio_ecommerce.entity.Enum.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductReviewService {
    Page<ProductReviewResponse> listStoreAllReviews(UUID storeId, Pageable pageable);

    ProductReviewResponse createReview(UUID currentCustomerId, ProductReviewCreateRequest req);

    ProductReviewResponse updateReview(UUID currentCustomerId, UUID reviewId, ProductReviewUpdateRequest req);

    void deleteReview(UUID currentCustomerId, UUID reviewId);

    // customer tự xem các review của mình (tất cả)
    Page<ProductReviewResponse> listMyReviews(UUID currentCustomerId, ReviewStatus status, Pageable pageable);

    // public trên trang product
    Page<ProductReviewResponse> listProductReviews(UUID productId, Pageable pageable);

    // store xem review của sản phẩm mình
    Page<ProductReviewResponse> listStoreProductReviews(UUID storeId, UUID productId, Pageable pageable);

    // store reply review
    ProductReviewResponse replyReview(UUID storeId, UUID reviewId, ProductReviewReplyRequest req);

    // customer xem review của mình trên 1 sản phẩm cụ thể
    ProductReviewResponse getMyReviewForProduct(UUID currentCustomerId, UUID productId);
}
