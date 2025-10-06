package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreResponse;
import org.example.audio_ecommerce.dto.response.UpdateStoreResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    @Override
    public ResponseEntity<BaseResponse> updateStore(UUID storeId, UpdateStoreRequest request) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // ✅ Cập nhật các trường nếu có giá trị mới
        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            store.setStoreName(request.getStoreName());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl());
        }
        if (request.getCoverImageUrl() != null) {
            store.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getAddress() != null) {
            store.setAddress(request.getAddress());
        }
        if (request.getPhoneNumber() != null) {
            store.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            store.setEmail(request.getEmail());
        }

        storeRepository.save(store);

        UpdateStoreResponse response = new UpdateStoreResponse(
                store.getStoreId(),
                store.getStoreName(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getCoverImageUrl(),
                store.getAddress(),
                store.getPhoneNumber(),
                store.getEmail()
        );

        return ResponseEntity.ok(
                new BaseResponse<>(200, "Store updated successfully", response)
        );
    }

    @Override
public ResponseEntity<BaseResponse> getStoreById(UUID storeId) {
    Store store = storeRepository.findByStoreId(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    StoreResponse response = StoreResponse.builder()
            .storeId(store.getStoreId())
            .storeName(store.getStoreName())
            .description(store.getDescription())
            .logoUrl(store.getLogoUrl())
            .coverImageUrl(store.getCoverImageUrl())
            .address(store.getAddress())
            .phoneNumber(store.getPhoneNumber())
            .email(store.getEmail())
            .rating(store.getRating())
            .status(store.getStatus())
            .accountId(store.getAccount().getId()) // lấy id từ BaseEntity
            .build();

    return ResponseEntity.ok(new BaseResponse<>(200, "Store found", response));
}

@Override
public ResponseEntity<BaseResponse> getStoreByAccountId(UUID accountId) {
    Store store = storeRepository.findByAccount_Id(accountId)
            .orElseThrow(() -> new RuntimeException("Store not found for this account"));

    StoreResponse response = StoreResponse.builder()
            .storeId(store.getStoreId())
            .storeName(store.getStoreName())
            .description(store.getDescription())
            .logoUrl(store.getLogoUrl())
            .coverImageUrl(store.getCoverImageUrl())
            .address(store.getAddress())
            .phoneNumber(store.getPhoneNumber())
            .email(store.getEmail())
            .rating(store.getRating())
            .status(store.getStatus())
            .accountId(store.getAccount().getId())
            .build();

    return ResponseEntity.ok(new BaseResponse<>(200, "Store found by account", response));
}

  @Override
    public ResponseEntity<BaseResponse> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        return ResponseEntity.ok(new BaseResponse<>(200, "List of all stores", stores));
    }

    @Override
    public ResponseEntity<BaseResponse> updateStoreStatus(UUID storeId, StoreStatus status) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        store.setStatus(status);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "Store status updated to " + status, store));
    }
}
