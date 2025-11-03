package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService comboService;

    // ========================== SHOP ==========================

    @PostMapping("/shop")
    public ResponseEntity<BaseResponse> createShopCombo(@RequestBody CreateShopComboRequest request) {
        return comboService.createShopCombo(request);
    }

    @PutMapping("/shop/{comboId}")
    public ResponseEntity<BaseResponse> updateShopCombo(
            @PathVariable UUID comboId,
            @RequestBody UpdateShopComboRequest request) {
        return comboService.updateShopCombo(comboId, request);
    }

    @GetMapping("/shop") // <<== remove path variable storeId
    public ResponseEntity<BaseResponse> viewShopCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        return comboService.viewShopCombos(page, size, keyword, isActive);
    }


    // ===================== CUSTOMER ===========================

    @PostMapping("/customer")
    public ResponseEntity<BaseResponse> createCustomerCombo(@RequestBody CreateCustomerComboRequest request) {
        return comboService.createCustomerCombo(request);
    }

    @PutMapping("/customer/{comboId}")
    public ResponseEntity<BaseResponse> updateCustomerCombo(
            @PathVariable UUID comboId,
            @RequestBody UpdateCustomerComboRequest request) {
                return comboService.updateCustomerCombo(comboId, request);
    }

    @GetMapping("/customer") // <<== remove path variable customerId
    public ResponseEntity<BaseResponse> viewCustomerCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        return comboService.viewCustomerCombos(page, size, keyword, isActive);
    }
}

