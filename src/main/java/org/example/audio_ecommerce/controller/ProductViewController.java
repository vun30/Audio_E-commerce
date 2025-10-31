package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductViewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products/view")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductViewService productViewService;

    @GetMapping
    public ResponseEntity<BaseResponse> getProductThumbnails(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) String wardCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return productViewService.getThumbnailView(
                status, categoryId, storeId, keyword,
                provinceCode, districtCode, wardCode, pageable
        );
    }
}
