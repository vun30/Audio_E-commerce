package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductReviewCreateRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewCreateSimpleRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewReplyRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewUpdateRequest;
import org.example.audio_ecommerce.dto.response.ProductReviewResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.NotificationCreatorService;
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
    private final NotificationCreatorService notificationCreatorService;


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

        Product productToUpdate = saved.getProduct();
        int current = nvlInt(productToUpdate.getReviewCount());
        productToUpdate.setReviewCount(current + 1);
        productRepo.save(productToUpdate);
        // 🔔 Thông báo cho STORE có review mới
        try {
            notificationCreatorService.createAndSend(
                    NotificationTarget.STORE,
                    store.getStoreId(),
                    NotificationType.NEW_REVIEW,
                    "Đánh giá mới cho sản phẩm " + product.getName(),
                    "Khách hàng " + customer.getFullName()
                            + " đã đánh giá " + req.getRating() + "★ cho sản phẩm " + product.getName(),
                    "/seller/products/" + product.getProductId() + "/reviews",
                    "{\"productId\":\"" + product.getProductId() + "\"}",
                    Map.of(
                            "screen", "SELLER_PRODUCT_REVIEWS",
                            "productId", String.valueOf(product.getProductId())
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        // chỉ trừ khi đang VISIBLE
        if (review.getStatus() == ReviewStatus.VISIBLE) {
            Product product = review.getProduct();
            int current = nvlInt(product.getReviewCount());

            // không cho âm
            product.setReviewCount(Math.max(0, current - 1));
            productRepo.save(product);
        }

        review.setStatus(ReviewStatus.DELETED);
        reviewRepo.save(review);
    }

    // ================== LIST MY ==================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listMyReviews(UUID currentCustomerId, ReviewStatus status,Pageable pageable) {
        if (status == null) {
            return reviewRepo.findByCustomer_Id(currentCustomerId, pageable)
                    .map(this::toResponse);
        }
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

        // 🔔 Thông báo cho CUSTOMER khi shop reply
        try {
            Customer customer = review.getCustomer();
            Product product = review.getProduct();

            notificationCreatorService.createAndSend(
                    NotificationTarget.CUSTOMER,
                    customer.getId(),
                    NotificationType.NEW_REVIEW_REPLY,
                    "Cửa hàng đã phản hồi đánh giá của bạn",
                    "Cửa hàng " + review.getStore().getStoreName()
                            + " đã trả lời đánh giá cho sản phẩm " + product.getName(),
                    "/customer/products/" + product.getProductId() + "/reviews",
                    "{\"productId\":\"" + product.getProductId() + "\"}",
                    Map.of(
                            "screen", "PRODUCT_REVIEW_DETAIL",
                            "productId", String.valueOf(product.getProductId())
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }


        return toResponse(review);
    }

    @Override
    @Transactional
    public ProductReviewResponse createReviewForProduct(UUID currentCustomerId, UUID productId, UUID orderId, ProductReviewCreateSimpleRequest req) {

        // tìm order item từ orderId + productId
        CustomerOrderItem item = orderItemRepo
                .findByCustomerOrder_IdAndRefIdAndType(orderId, productId, "PRODUCT")
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm này trong đơn"));

        CustomerOrder order = item.getCustomerOrder();
        if (!order.getCustomer().getId().equals(currentCustomerId)) {
            throw new IllegalStateException("Không thể review đơn hàng của người khác");
        }

        if (order.getStatus() != OrderStatus.DELIVERY_SUCCESS) {
            throw new IllegalStateException("Chỉ được review khi đơn đã DELIVERY_SUCCESS");
        }

        // check đã review chưa
        reviewRepo.findByOrderItem_IdAndCustomer_Id(item.getId(), currentCustomerId)
                .ifPresent(r -> {
                    throw new IllegalStateException("Bạn đã review sản phẩm này trong đơn hàng này rồi");
                });

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        Store store = product.getStore();

        ProductReview review = ProductReview.builder()
                .customer(order.getCustomer())
                .product(product)
                .store(store)
                .orderItem(item)
                .rating(req.getRating())
                .content(req.getContent())
                .status(ReviewStatus.VISIBLE)
                .variantOptionName(item.getVariantOptionName())
                .variantOptionValue(item.getVariantOptionValue())
                .build();

        if (req.getMedia() != null) {
            List<ProductReviewMedia> mediaList = req.getMedia().stream()
                    .map(m -> ProductReviewMedia.builder()
                            .review(review)
                            .type(ReviewMediaType.valueOf(m.getType().toUpperCase()))
                            .url(m.getUrl())
                            .build())
                    .toList();

            review.setMediaList(mediaList);
        }

        ProductReview saved = reviewRepo.save(review);

        Product productToUpdate = saved.getProduct();
        int current = nvlInt(productToUpdate.getReviewCount());
        productToUpdate.setReviewCount(current + 1);
        productRepo.save(productToUpdate);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> checkMyReviewStatus(UUID currentCustomerId, UUID productId, UUID orderId) {

        CustomerOrderItem item = orderItemRepo
                .findByCustomerOrder_IdAndRefIdAndType(orderId, productId, "PRODUCT")
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm trong đơn"));

        Optional<ProductReview> reviewOpt =
                reviewRepo.findByOrderItem_IdAndCustomer_Id(item.getId(), currentCustomerId);

        Map<String, Object> result = new HashMap<>();

        if (reviewOpt.isPresent()) {
            result.put("hasReviewed", true);
            result.put("message", "Sản phẩm trong đơn hàng này đã được review");
            result.put("review", toResponse(reviewOpt.get()));
        } else {
            result.put("hasReviewed", false);
            result.put("message", "Bạn chưa review sản phẩm này");
            result.put("review", null);
        }

        return result;
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
        return reviewRepo.findByProduct_ProductIdAndCustomer_Id(productId, currentCustomerId)
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
                .status(r.getStatus().name())
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

    private int nvlInt(Integer v) {
        return v == null ? 0 : v;
    }

}
