package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest.StoreAddressRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreResponse;
import org.example.audio_ecommerce.dto.response.UpdateStoreResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    // =========================================================
    // 🔐 Lấy store theo accountId trong JWT (hiện tại đang login)
    // =========================================================
    private Store getCurrentUserStore() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String principal = auth.getName(); // ví dụ: "baohsse173095@fpt.edu.vn:STOREOWNER"

    UUID accountId = null;

    try {
        // ✅ Nếu Authentication lưu dạng "email:ROLE:UUID"
        if (principal.contains(":")) {
            String[] parts = principal.split(":");
            for (String p : parts) {
                try {
                    accountId = UUID.fromString(p);
                    break;
                } catch (IllegalArgumentException ignored) {}
            }
        }
    } catch (Exception ignored) {}

    // ✅ Nếu không parse được accountId → fallback qua email
    if (accountId == null) {
        String email = principal.contains(":") ? principal.split(":")[0] : principal;
        return storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user (email=" + email + ")"));
    }

    // ✅ Ưu tiên tìm bằng accountId (dùng method có sẵn trong repo)
    UUID finalAccountId = accountId; // cần final để lambda không lỗi
    return storeRepository.findByAccount_Id(finalAccountId)
            .orElseThrow(() -> new RuntimeException("❌ Store not found for current user (accountId=" + finalAccountId + ")"));
}

    // =========================================================
    // ✏️ UPDATE STORE
    // =========================================================
    @Override
    public ResponseEntity<BaseResponse> updateStore(UUID storeId, UpdateStoreRequest request) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Store not found"));

        // ✅ Cập nhật thông tin cơ bản
        if (request.getStoreName() != null && !request.getStoreName().isBlank()) store.setStoreName(request.getStoreName());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) store.setLogoUrl(request.getLogoUrl());
        if (request.getCoverImageUrl() != null) store.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getAddress() != null) store.setAddress(request.getAddress());
        if (request.getPhoneNumber() != null) store.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) store.setEmail(request.getEmail());

        // ✅ Cập nhật danh sách địa chỉ nếu có
        if (request.getStoreAddresses() != null) {
            store.setStoreAddresses(
                    request.getStoreAddresses().stream()
                            .map(a -> new Store.StoreAddress(
                                    a.getDefaultAddress() != null && a.getDefaultAddress(),
                                    a.getProvinceCode(),
                                    a.getDistrictCode(),
                                    a.getWardCode(),
                                    a.getAddress(),
                                    a.getAddressLocation()
                            ))
                            .toList()
            );
            ensureSingleDefault(store);
        }

        storeRepository.save(store);

        // ✅ Build response
        UpdateStoreResponse response = UpdateStoreResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .description(store.getDescription())
                .logoUrl(store.getLogoUrl())
                .coverImageUrl(store.getCoverImageUrl())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .email(store.getEmail())
                .storeAddresses(
                        store.getStoreAddresses() != null
                                ? store.getStoreAddresses().stream()
                                .map(a -> new UpdateStoreResponse.StoreAddressResponse(
                                        Boolean.TRUE.equals(a.getDefaultAddress()),
                                        a.getProvinceCode(),
                                        a.getDistrictCode(),
                                        a.getWardCode(),
                                        a.getAddress(),
                                        a.getAddressLocation()
                                ))
                                .toList()
                                : null
                )
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Store updated successfully", response));
    }

    // =========================================================
    // ➕ ADD ADDRESS
    // =========================================================
    public ResponseEntity<BaseResponse> addStoreAddress(StoreAddressRequest req) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null)
            store.setStoreAddresses(new ArrayList<>());

        Store.StoreAddress newAddr = new Store.StoreAddress(
                req.getDefaultAddress() != null && req.getDefaultAddress(),
                req.getProvinceCode(),
                req.getDistrictCode(),
                req.getWardCode(),
                req.getAddress(),
                req.getAddressLocation()
        );

        // Nếu là địa chỉ mặc định thì bỏ mặc định ở địa chỉ khác
        if (Boolean.TRUE.equals(newAddr.getDefaultAddress())) {
            store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));
        }

        store.getStoreAddresses().add(newAddr);
        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "🏠 Address added successfully", store.getStoreAddresses()));
    }

    // =========================================================
    // ✏️ UPDATE ADDRESS
    // =========================================================
    public ResponseEntity<BaseResponse> updateStoreAddress(int index, StoreAddressRequest req) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || index < 0 || index >= store.getStoreAddresses().size())
            throw new RuntimeException("❌ Invalid address index");

        Store.StoreAddress addr = store.getStoreAddresses().get(index);

        if (req.getProvinceCode() != null) addr.setProvinceCode(req.getProvinceCode());
        if (req.getDistrictCode() != null) addr.setDistrictCode(req.getDistrictCode());
        if (req.getWardCode() != null) addr.setWardCode(req.getWardCode());
        if (req.getAddress() != null) addr.setAddress(req.getAddress());
        if (req.getAddressLocation() != null) addr.setAddressLocation(req.getAddressLocation());

        if (req.getDefaultAddress() != null && req.getDefaultAddress()) {
            store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));
            addr.setDefaultAddress(true);
        }

        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Address updated successfully", addr));
    }

    // =========================================================
    // ❌ DELETE ADDRESS
    // =========================================================
    public ResponseEntity<BaseResponse> deleteStoreAddress(int index) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || index < 0 || index >= store.getStoreAddresses().size())
            throw new RuntimeException("❌ Invalid address index");

        store.getStoreAddresses().remove(index);
        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "🗑️ Address deleted successfully", store.getStoreAddresses()));
    }

    // =========================================================
    // 🌟 SET DEFAULT ADDRESS
    // =========================================================
    public ResponseEntity<BaseResponse> setDefaultAddress(int index) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || index < 0 || index >= store.getStoreAddresses().size())
            throw new RuntimeException("❌ Invalid address index");

        store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));
        store.getStoreAddresses().get(index).setDefaultAddress(true);

        storeRepository.save(store);
        return ResponseEntity.ok(new BaseResponse<>(200, "🌟 Default address set successfully", store.getStoreAddresses()));
    }

    // =========================================================
    // 📋 GET ALL ADDRESSES
    // =========================================================
    public ResponseEntity<BaseResponse> getAllAddresses() {
        Store store = getCurrentUserStore();
        return ResponseEntity.ok(new BaseResponse<>(200, "📋 Addresses retrieved", store.getStoreAddresses()));
    }

    // =========================================================
    // ⚙️ Chỉ 1 địa chỉ mặc định
    // =========================================================
    private void ensureSingleDefault(Store store) {
        if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty()) return;

        boolean found = false;
        for (Store.StoreAddress addr : store.getStoreAddresses()) {
            if (Boolean.TRUE.equals(addr.getDefaultAddress())) {
                if (!found) found = true;
                else addr.setDefaultAddress(false);
            }
        }

        if (!found) store.getStoreAddresses().get(0).setDefaultAddress(true);
    }

    // =========================================================
    // ⚙️ Các hàm cũ giữ nguyên
    // =========================================================
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
                .accountId(store.getAccount().getId())
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
    public ResponseEntity<BaseResponse> updateStoreStatus(UUID storeId, StoreStatus status) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        store.setStatus(status);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "Store status updated to " + status, store));
    }

    @Override
    public ResponseEntity<BaseResponse> getAllStores(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Store> storePage = (keyword != null && !keyword.isBlank())
                ? storeRepository.findByStoreNameContainingIgnoreCase(keyword, pageable)
                : storeRepository.findAll(pageable);

        List<StoreResponse> storeResponses = storePage.getContent().stream().map(store ->
                StoreResponse.builder()
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
                        .build()
        ).toList();

        Map<String, Object> response = Map.of(
                "currentPage", storePage.getNumber(),
                "totalPages", storePage.getTotalPages(),
                "totalElements", storePage.getTotalElements(),
                "stores", storeResponses
        );

        return ResponseEntity.ok(new BaseResponse<>(200, "Get stores successfully", response));
    }

    @Override
    public Optional<Store> getStoreByEmail(String email) {
        return storeRepository.findByAccount_Email(email);
    }
}
