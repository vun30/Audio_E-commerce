package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductReviewCreateRequest;
import org.example.audio_ecommerce.dto.request.ProductReviewUpdateRequest;
import org.example.audio_ecommerce.dto.response.ProductReviewResponse;
import org.example.audio_ecommerce.service.ProductReviewService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Review Customer", description = "CRUD review sản phẩm bởi customer và get review công khai của product đó")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;
    private final SecurityUtils securityUtils;

    // ===== CUSTOMER: create review =====
    @PostMapping
    public ProductReviewResponse create(@Valid @RequestBody ProductReviewCreateRequest req) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        return reviewService.createReview(customerId, req);
    }

    // ===== CUSTOMER: update review =====
    @PutMapping("/{id}")
    public ProductReviewResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductReviewUpdateRequest req) {

        UUID customerId = securityUtils.getCurrentCustomerId();
        return reviewService.updateReview(customerId, id, req);
    }

    // ===== CUSTOMER: delete review =====
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        reviewService.deleteReview(customerId, id);
    }

    // ===== CUSTOMER: list các review của chính mình =====
    @GetMapping("/me")
    public Page<ProductReviewResponse> listMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID customerId = securityUtils.getCurrentCustomerId();
        Pageable pageable = PageRequest.of(page, size);

        return reviewService.listMyReviews(customerId, pageable);
    }

    // ===== PUBLIC: get all review cho 1 sản phẩm =====
    @GetMapping("/product/{productId}")
    public Page<ProductReviewResponse> listProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return reviewService.listProductReviews(productId, pageable);
    }

    // ===== CUSTOMER: xem review của mình trên 1 sản phẩm =====
    @GetMapping("/product/{productId}/me")
    public ProductReviewResponse getMyReviewForProduct(@PathVariable UUID productId) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        return reviewService.getMyReviewForProduct(customerId, productId);
    }
}
