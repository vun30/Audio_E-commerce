package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreStatusRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // ✅ View store theo ID
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse> getStoreById(@PathVariable UUID storeId) {
        return storeService.getStoreById(storeId);
    }

    // ✅ View store theo accountId (dùng cho dashboard chủ shop)
    @GetMapping("/account/{accountId}")
    public ResponseEntity<BaseResponse> getStoreByAccount(@PathVariable UUID accountId) {
        return storeService.getStoreByAccountId(accountId);
    }

    // ✅ Update store
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse> updateStore(
            @PathVariable UUID storeId,
            @RequestBody UpdateStoreRequest request
    ) {
        return storeService.updateStore(storeId, request);
    }

        // ✅ Lấy danh sách tất cả store (cho admin)
    @GetMapping
    public ResponseEntity<BaseResponse> getAllStores() {
        return storeService.getAllStores();
    }

     // ✅ Thay đổi trạng thái store (ACTIVE, INACTIVE, PENDING, REJECTED)
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse> updateStoreStatus(
            @PathVariable UUID storeId,
            @RequestBody UpdateStoreStatusRequest request
    ) {
        return storeService.updateStoreStatus(storeId, request.getStatus());
    }


}
