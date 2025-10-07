package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductComboService {
    ResponseEntity<BaseResponse> createCombo(CreateComboRequest request);
    ResponseEntity<BaseResponse> getComboById(UUID comboId);
    ResponseEntity<BaseResponse> getAllCombos(int page, int size, String keyword, String sortDir, BigDecimal minPrice, BigDecimal maxPrice);
    ResponseEntity<BaseResponse> getCombosByStoreId(UUID storeId, int page, int size, String keyword, String sortDir, BigDecimal minPrice, BigDecimal maxPrice);
    ResponseEntity<BaseResponse> updateCombo(UUID comboId, UpdateComboRequest request);
    ResponseEntity<BaseResponse> disableCombo(UUID comboId);
    ResponseEntity<BaseResponse> getProductsInCombo(UUID comboId);
}