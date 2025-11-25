package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductReviewReplyRequest;
import org.example.audio_ecommerce.dto.response.ProductReviewResponse;
import org.example.audio_ecommerce.service.ProductReviewService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Review Store", description = "Quản lý review của store, bao gồm xem và trả lời review")
@RestController
@RequestMapping("/api/store/reviews")
@RequiredArgsConstructor
public class StoreProductReviewController {

    private final ProductReviewService reviewService;
    private final SecurityUtils securityUtils;

    // ===== STORE: xem tất cả review của 1 sản phẩm =====
    @GetMapping("/product/{productId}")
    public Page<ProductReviewResponse> listStoreProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID storeId = securityUtils.getCurrentStoreId();
        Pageable pageable = PageRequest.of(page, size);

        return reviewService.listStoreProductReviews(storeId, productId, pageable);
    }

    // ===== STORE: reply review =====
    @PostMapping("/{reviewId}/reply")
    public ProductReviewResponse reply(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ProductReviewReplyRequest req) {

        UUID storeId = securityUtils.getCurrentStoreId();
        return reviewService.replyReview(storeId, reviewId, req);
    }

    @GetMapping
    public Page<ProductReviewResponse> listStoreAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID storeId = securityUtils.getCurrentStoreId();
        Pageable pageable = PageRequest.of(page, size);

        return reviewService.listStoreAllReviews(storeId, pageable);
    }
}
