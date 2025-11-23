package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductReviewCreateRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewReplyRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewUpdateRequest;
import org.example.audio_ecommerce.dto.response.ProductReviewResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Enum.ReviewMediaType;
import org.example.audio_ecommerce.entity.Enum.ReviewStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.ProductReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductReviewReplyRepository replyRepo;

    private final CustomerOrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final StoreRepository storeRepo;

    // ================== CREATE ==================
    @Override
    @Transactional
    public ProductReviewResponse createReview(UUID currentCustomerId, ProductReviewCreateRequest req) {
        CustomerOrderItem item = orderItemRepo.findById(req.getCustomerOrderItemId())
                .orElseThrow(() -> new NoSuchElementException("Order item not found"));

        CustomerOrder order = item.getCustomerOrder();
        if (!order.getCustomer().getId().equals(currentCustomerId)) {
            throw new IllegalStateException("Không thể review đơn hàng của người khác");
        }

        if (order.getStatus() != OrderStatus.DELIVERY_SUCCESS) {
            throw new IllegalStateException("Chỉ được review khi đơn đã DELIVERY_SUCCESS");
        }

        // chỉ cho review PRODUCT
        if (!"PRODUCT".equalsIgnoreCase(item.getType())) {
            throw new IllegalStateException("Chỉ hỗ trợ review PRODUCT");
        }

        // 1 item chỉ review 1 lần
        reviewRepo.findByOrderItem_IdAndCustomer_Id(item.getId(), currentCustomerId)
                .ifPresent(r -> {
                    throw new IllegalStateException("Bạn đã review item này rồi");
                });

        Customer customer = order.getCustomer();
        Product product = productRepo.findById(item.getRefId())
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        Store store = product.getStore();
        if (store == null) {
            store = storeRepo.findById(item.getStoreId())
                    .orElseThrow(() -> new NoSuchElementException("Store not found"));
        }

        // ❗ review KHÔNG bị gán lại sau lambda → effectively final
        ProductReview review = ProductReview.builder()
                .customer(customer)
                .product(product)
                .store(store)
                .orderItem(item)
                .rating(req.getRating())
                .content(req.getContent())
                .status(ReviewStatus.VISIBLE)
                .variantOptionName(item.getVariantOptionName())
                .variantOptionValue(item.getVariantOptionValue())
                .build();

        // media
        if (req.getMedia() != null) {
            List<ProductReviewMedia> mediaList = req.getMedia().stream()
                    .map(m -> ProductReviewMedia.builder()
                            .review(review)
                            .type(ReviewMediaType.valueOf(m.getType().toUpperCase()))
                            .url(m.getUrl())
                            .build())
                    .collect(Collectors.toList());
            review.setMediaList(mediaList);
        }

        ProductReview saved = reviewRepo.save(review);
        return toResponse(saved);
    }

    // ================== UPDATE ==================
    @Override
    @Transactional
    public ProductReviewResponse updateReview(UUID currentCustomerId, UUID reviewId, ProductReviewUpdateRequest req) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found"));

        if (!review.getCustomer().getId().equals(currentCustomerId)) {
            throw new IllegalStateException("Không thể sửa review của người khác");
        }

        review.setRating(req.getRating());
        review.setContent(req.getContent());

        // clear media cũ (orphanRemoval = true sẽ tự xoá)
        if (review.getMediaList() != null) {
            review.getMediaList().clear();
        }

        if (req.getMedia() != null) {
            List<ProductReviewMedia> mediaList = req.getMedia().stream()
                    .map(m -> ProductReviewMedia.builder()
                            .review(review)
                            .type(ReviewMediaType.valueOf(m.getType().toUpperCase()))
                            .url(m.getUrl())
                            .build())
                    .collect(Collectors.toList());
            review.setMediaList(mediaList);
        }

        // không gán lại biến review → không dính lỗi effectively final
        reviewRepo.save(review);
        return toResponse(review);
    }

    // ================== DELETE (soft) ==================
    @Override
    @Transactional
    public void deleteReview(UUID currentCustomerId, UUID reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found"));

        if (!review.getCustomer().getId().equals(currentCustomerId)) {
            throw new IllegalStateException("Không thể xoá review của người khác");
        }

        review.setStatus(ReviewStatus.DELETED);
        reviewRepo.save(review);
    }

    // ================== LIST MY ==================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listMyReviews(UUID currentCustomerId, Pageable pageable) {
        return reviewRepo.findByCustomer_Id(currentCustomerId, pageable)
                .map(this::toResponse);
    }

    // ================== LIST PRODUCT (public) ==================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listProductReviews(UUID productId, Pageable pageable) {
        return reviewRepo.findByProduct_ProductIdAndStatus(productId, ReviewStatus.VISIBLE, pageable)
                .map(this::toResponse);
    }

    // ================== LIST PRODUCT FOR STORE ==================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listStoreProductReviews(UUID storeId, UUID productId, Pageable pageable) {
        return reviewRepo.findByProduct_ProductIdAndStore_StoreIdAndStatus(
                        productId, storeId, ReviewStatus.VISIBLE, pageable)
                .map(this::toResponse);
    }

    // ================== REPLY ==================
    @Override
    @Transactional
    public ProductReviewResponse replyReview(UUID storeId, UUID reviewId, ProductReviewReplyRequest req) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found"));

        if (!review.getStore().getStoreId().equals(storeId)) {
            throw new IllegalStateException("Review này không thuộc store của bạn");
        }

        ProductReviewReply reply = ProductReviewReply.builder()
                .review(review)
                .store(review.getStore())
                .content(req.getContent())
                .build();

        replyRepo.save(reply);

        review.getReplies().add(reply);
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listStoreAllReviews(UUID storeId, Pageable pageable) {
        return reviewRepo.findByStore_StoreIdAndStatus(storeId, ReviewStatus.VISIBLE, pageable)
                .map(this::toResponse);
    }

    // ================== MY REVIEW FOR PRODUCT ==================
    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getMyReviewForProduct(UUID currentCustomerId, UUID productId) {
        return reviewRepo.findByProduct_ProductIdAndCustomer_IdAndStatus(
                        productId, currentCustomerId, ReviewStatus.VISIBLE)
                .map(this::toResponse)
                .orElse(null);
    }

    // ================== Mapper ==================
    private ProductReviewResponse toResponse(ProductReview r) {
        return ProductReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getFullName())
                .customerAvatarUrl(r.getCustomer().getAvatarURL())
                .productId(r.getProduct().getProductId())
                .variantOptionName(r.getVariantOptionName())
                .variantOptionValue(r.getVariantOptionValue())
                .media(Optional.ofNullable(r.getMediaList()).orElse(List.of())
                        .stream()
                        .map(m -> ProductReviewResponse.ReviewMediaResponse.builder()
                                .type(m.getType().name())
                                .url(m.getUrl())
                                .build())
                        .collect(Collectors.toList()))
                .replies(Optional.ofNullable(r.getReplies()).orElse(List.of())
                        .stream()
                        .map(rep -> ProductReviewResponse.ReviewReplyResponse.builder()
                                .storeName(rep.getStore().getStoreName())
                                .content(rep.getContent())
                                .createdAt(rep.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
