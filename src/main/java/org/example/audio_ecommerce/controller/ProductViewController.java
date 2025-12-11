package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductViewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/view")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductViewService productViewService;

    // ================================
    // 🖼️ 1) Thumbnail list + Filters
    // ================================
    @GetMapping
    public ResponseEntity<BaseResponse> getProductThumbnails(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) String wardCode,

            // 🔥 Filter bổ sung
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,

            // Paging
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return productViewService.getThumbnailView(
                status,
                categoryId,
                storeId,
                keyword,
                provinceCode,
                districtCode,
                wardCode,
                minPrice,
                maxPrice,
                minRating,
                pageable
        );
    }

    // ================================
    // 🎯 2) PDP – Active vouchers
    // ================================
    @GetMapping("/{productId}/vouchers")
    public ResponseEntity<BaseResponse> getProductVouchers(
            @PathVariable UUID productId,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) String campaignType
    ) {
        return productViewService.getActiveVouchersOfProduct(productId, type, campaignType);
    }
}
