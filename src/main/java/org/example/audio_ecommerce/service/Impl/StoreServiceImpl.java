package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest;
import org.example.audio_ecommerce.dto.request.UpdateStoreRequest.StoreAddressRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreResponse;
import org.example.audio_ecommerce.dto.response.UpdateStoreResponse;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreAddressEntity;
import org.example.audio_ecommerce.repository.ProductRepository;
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
    private final ProductRepository productRepository;

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
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

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
        if (request.getStoreName() != null && !request.getStoreName().isBlank())
            store.setStoreName(request.getStoreName());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) store.setLogoUrl(request.getLogoUrl());
        if (request.getCoverImageUrl() != null) store.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getAddress() != null) store.setAddress(request.getAddress());
        if (request.getPhoneNumber() != null) store.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) store.setEmail(request.getEmail());

        // ✅ Cập nhật danh sách địa chỉ nếu có (REPLACE FULL LIST)
        if (request.getStoreAddresses() != null) {
            if (store.getStoreAddresses() == null) {
                store.setStoreAddresses(new ArrayList<>());
            }

            // Xóa list cũ trong entity (orphanRemoval sẽ lo phần delete)
            store.getStoreAddresses().clear();

            // Thêm lại theo request
            for (StoreAddressRequest a : request.getStoreAddresses()) {
                StoreAddressEntity addr = StoreAddressEntity.builder()
                        .id(a.getAddressId()) // có thể null → JPA tự generate
                        .defaultAddress(a.getDefaultAddress() != null && a.getDefaultAddress())
                        .provinceCode(a.getProvinceCode())
                        .districtCode(a.getDistrictCode())
                        .wardCode(a.getWardCode())
                        .address(a.getAddress())
                        .addressLocation(a.getAddressLocation())
                        .store(store)
                        .build();

                store.getStoreAddresses().add(addr);
            }

            ensureSingleDefault(store);
        }

        storeRepository.save(store);

        // ✅ Build response
        List<UpdateStoreResponse.StoreAddressResponse> addressResponses = null;
        if (store.getStoreAddresses() != null) {
            addressResponses = store.getStoreAddresses().stream()
                    .map(a -> new UpdateStoreResponse.StoreAddressResponse(
                            a.getId(),
                            Boolean.TRUE.equals(a.getDefaultAddress()),
                            a.getProvinceCode(),
                            a.getDistrictCode(),
                            a.getWardCode(),
                            a.getAddress(),
                            a.getAddressLocation()
                    ))
                    .toList();
        }

        UpdateStoreResponse response = UpdateStoreResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .description(store.getDescription())
                .logoUrl(store.getLogoUrl())
                .coverImageUrl(store.getCoverImageUrl())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .email(store.getEmail())
                .storeAddresses(addressResponses)
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Store updated successfully", response));
    }

    // =========================================================
    // ➕ ADD ADDRESS (dùng index logic cũ nhưng với StoreAddressEntity)
    // =========================================================
    public ResponseEntity<BaseResponse> addStoreAddress(StoreAddressRequest req) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null)
            store.setStoreAddresses(new ArrayList<>());

        StoreAddressEntity newAddr = StoreAddressEntity.builder()
                .defaultAddress(req.getDefaultAddress() != null && req.getDefaultAddress())
                .provinceCode(req.getProvinceCode())
                .districtCode(req.getDistrictCode())
                .wardCode(req.getWardCode())
                .address(req.getAddress())
                .addressLocation(req.getAddressLocation())
                .store(store)
                .build();

        // Nếu là địa chỉ mặc định thì bỏ mặc định ở địa chỉ khác
        if (Boolean.TRUE.equals(newAddr.getDefaultAddress())) {
            store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));
        }

        store.getStoreAddresses().add(newAddr);
        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "🏠 Address added successfully", store.getStoreAddresses()));
    }

    public ResponseEntity<BaseResponse> updateStoreAddress(UUID addressId, StoreAddressRequest req) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty())
            throw new RuntimeException("❌ Store has no addresses");

        // 🔍 tìm địa chỉ theo ID
        StoreAddressEntity addr = store.getStoreAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("❌ Address not found"));

        // ✏️ Update
        if (req.getProvinceCode() != null) addr.setProvinceCode(req.getProvinceCode());
        if (req.getDistrictCode() != null) addr.setDistrictCode(req.getDistrictCode());
        if (req.getWardCode() != null) addr.setWardCode(req.getWardCode());
        if (req.getAddress() != null) addr.setAddress(req.getAddress());
        if (req.getAddressLocation() != null) addr.setAddressLocation(req.getAddressLocation());

        // Nếu được chọn làm default → bỏ default cũ
        if (req.getDefaultAddress() != null && req.getDefaultAddress()) {
            store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));
            addr.setDefaultAddress(true);
        }

        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Address updated successfully", addr));
    }

    public ResponseEntity<BaseResponse> deleteStoreAddress(UUID addressId) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty())
            throw new RuntimeException("❌ Store has no addresses");

        boolean removed = store.getStoreAddresses().removeIf(a -> a.getId().equals(addressId));

        if (!removed) {
            throw new RuntimeException("❌ Address not found");
        }

        ensureSingleDefault(store);
        storeRepository.save(store);

        return ResponseEntity.ok(new BaseResponse<>(200, "🗑️ Address deleted successfully", store.getStoreAddresses()));
    }

    public ResponseEntity<BaseResponse> setDefaultAddress(UUID addressId) {
        Store store = getCurrentUserStore();

        if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty())
            throw new RuntimeException("❌ Store has no addresses");

        StoreAddressEntity target = store.getStoreAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("❌ Address not found"));

        // Bỏ mặc định ở những cái khác
        store.getStoreAddresses().forEach(a -> a.setDefaultAddress(false));

        // Set mặc định
        target.setDefaultAddress(true);

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
        for (StoreAddressEntity addr : store.getStoreAddresses()) {
            if (Boolean.TRUE.equals(addr.getDefaultAddress())) {
                if (!found) {
                    found = true;
                } else {
                    addr.setDefaultAddress(false);
                }
            }
        }

        if (!found) store.getStoreAddresses().get(0).setDefaultAddress(true);
    }

    // =========================================================
    // ⚙️ Các hàm cũ giữ nguyên (chỉ map thêm addressId nếu cần)
    // =========================================================
    @Override
    public ResponseEntity<BaseResponse> getStoreById(UUID storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        List<StoreResponse.StoreAddressResponse> addrResponses = null;
        if (store.getStoreAddresses() != null) {
            addrResponses = store.getStoreAddresses().stream()
                    .map(a -> new StoreResponse.StoreAddressResponse(
                            a.getId(),
                            a.getDefaultAddress(),
                            a.getProvinceCode(),
                            a.getDistrictCode(),
                            a.getWardCode(),
                            a.getAddress(),
                            a.getAddressLocation()
                    ))
                    .toList();
        }

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
                .storeAddresses(addrResponses)
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "Store found", response));
    }

    @Override
    public ResponseEntity<BaseResponse> getStoreByAccountId(UUID accountId) {
        Store store = storeRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new RuntimeException("Store not found for this account"));

        List<StoreResponse.StoreAddressResponse> addrResponses = null;
        if (store.getStoreAddresses() != null) {
            addrResponses = store.getStoreAddresses().stream()
                    .map(a -> new StoreResponse.StoreAddressResponse(
                            a.getId(),
                            a.getDefaultAddress(),
                            a.getProvinceCode(),
                            a.getDistrictCode(),
                            a.getWardCode(),
                            a.getAddress(),
                            a.getAddressLocation()
                    ))
                    .toList();
        }

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
                .storeAddresses(addrResponses)
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
                        // listing không cần list địa chỉ chi tiết
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

    @Override
    public ResponseEntity<BaseResponse> searchStores(String keyword, int page, int size) {

        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.ok(BaseResponse.error("❌ Keyword cannot be empty"));
        }

        // Chuẩn hóa keyword trước khi lọc
        keyword = keyword.trim().toLowerCase();
        String finalKeyword = keyword; // 🔥 cần cho lambda

        Pageable pageable = PageRequest.of(page, size);

        // Lấy danh sách store có chứa keyword (thô)
        Page<Store> stores = storeRepository.findByStoreNameContainingIgnoreCase(keyword, pageable);

        // 🔥 Lọc sạch: chỉ lấy store bắt đầu bằng keyword
        List<Store> filtered = stores.getContent().stream()
                .filter(s -> s.getStoreName() != null &&
                        s.getStoreName().toLowerCase().startsWith(finalKeyword))
                .toList();

        // Map dữ liệu trả về
        List<Map<String, Object>> results = filtered.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storeId", s.getStoreId());
            m.put("storeName", s.getStoreName());
            m.put("logoUrl", s.getLogoUrl());
            m.put("email", s.getEmail());
            m.put("phoneNumber", s.getPhoneNumber());
            m.put("status", s.getStatus());
            m.put("rating", s.getRating());

            // Default Address
            if (s.getStoreAddresses() != null && !s.getStoreAddresses().isEmpty()) {
                StoreAddressEntity defaultAddress = s.getStoreAddresses().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getDefaultAddress()))
                        .findFirst()
                        .orElse(s.getStoreAddresses().get(0));

                m.put("provinceCode", defaultAddress.getProvinceCode());
                m.put("districtCode", defaultAddress.getDistrictCode());
                m.put("wardCode", defaultAddress.getWardCode());
                m.put("address", defaultAddress.getAddress());
                m.put("addressId", defaultAddress.getId());
            }

            return m;
        }).toList();

        // Build pagination chuẩn
        Map<String, Object> pagination = Map.of(
                "pageNumber", page,
                "pageSize", size,
                "totalElements", filtered.size(),
                "totalPages", (int) Math.ceil((double) filtered.size() / size)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stores", results);
        response.put("pagination", pagination);

        return ResponseEntity.ok(BaseResponse.success(
                "🔍 Kết quả tìm kiếm cửa hàng (prefix match)",
                response
        ));
    }

    @Override
public ResponseEntity<BaseResponse> getDefaultAddressByProductId(UUID productId) {

    // 1. Lấy store từ productId
    Store store = productRepository.findStoreByProductId(productId)
            .orElseThrow(() -> new RuntimeException("❌ Store not found for this product"));

    // 2. Kiểm tra list address
    if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty()) {
        return ResponseEntity.ok(BaseResponse.error("❌ Store has no addresses"));
    }

    // 3. Tìm địa chỉ mặc định
    StoreAddressEntity defaultAddress = store.getStoreAddresses().stream()
            .filter(a -> Boolean.TRUE.equals(a.getDefaultAddress()))
            .findFirst()
            .orElse(store.getStoreAddresses().get(0)); // fallback

    // 4. Build response object
    Map<String, Object> result = Map.of(
            "storeId", store.getStoreId(),
            "productId", productId,
            "addressId", defaultAddress.getId(),
            "provinceCode", defaultAddress.getProvinceCode(),
            "districtCode", defaultAddress.getDistrictCode(),
            "wardCode", defaultAddress.getWardCode(),
            "address", defaultAddress.getAddress(),
            "location", defaultAddress.getAddressLocation()
    );

    return ResponseEntity.ok(new BaseResponse<>(200, "📦 Default store address retrieved", result));
}


}
