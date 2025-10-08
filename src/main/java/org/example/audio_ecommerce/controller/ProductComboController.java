package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService comboService;

    @PostMapping
    public ResponseEntity<BaseResponse> createCombo(@RequestBody CreateComboRequest request) {
        return comboService.createCombo(request);
    }

    @GetMapping("/{comboId}")
    public ResponseEntity<BaseResponse> getComboById(@PathVariable UUID comboId) {
        return comboService.getComboById(comboId);
    }

    @GetMapping
public ResponseEntity<BaseResponse> getAllCombos(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortDir,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean isActive // 👈 thêm param trạng thái
) {
    return comboService.getAllCombos(page, size, keyword, sortDir, minPrice, maxPrice, isActive);
}

    @GetMapping("/store/{storeId}")
    public ResponseEntity<BaseResponse> getCombosByStore(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return comboService.getCombosByStoreId(storeId, page, size, keyword, sortDir, minPrice, maxPrice);
    }

    @PutMapping("/{comboId}")
    public ResponseEntity<BaseResponse> updateCombo(@PathVariable UUID comboId,
                                                    @RequestBody UpdateComboRequest request) {
        return comboService.updateCombo(comboId, request);
    }

    @PutMapping("/{comboId}/disable")
    public ResponseEntity<BaseResponse> disableCombo(@PathVariable UUID comboId) {
        return comboService.disableCombo(comboId);
    }

    @GetMapping("/{comboId}/products")
    public ResponseEntity<BaseResponse> getProductsInCombo(@PathVariable UUID comboId) {
        return comboService.getProductsInCombo(comboId);
    }
}
