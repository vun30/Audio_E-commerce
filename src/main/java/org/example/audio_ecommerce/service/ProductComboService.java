package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ProductComboService {

    // SHOP
    ResponseEntity<BaseResponse> createShopCombo(CreateShopComboRequest request);
    ResponseEntity<BaseResponse> updateShopCombo(UUID comboId, UpdateShopComboRequest request);
    ResponseEntity<BaseResponse> viewShopCombos(int page, int size, String keyword, Boolean isActive);

    // CUSTOMER
    ResponseEntity<BaseResponse> createCustomerCombo(CreateCustomerComboRequest request);
    ResponseEntity<BaseResponse> updateCustomerCombo(UUID comboId, UpdateCustomerComboRequest request);
    ResponseEntity<BaseResponse> viewCustomerCombos(int page, int size, String keyword, Boolean isActive);

}
