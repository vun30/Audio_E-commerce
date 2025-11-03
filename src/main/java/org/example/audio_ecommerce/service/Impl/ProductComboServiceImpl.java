package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ProductComboResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.ComboCreatorType;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.repository.ProductComboRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductComboServiceImpl implements ProductComboService {

    private final ProductComboRepository comboRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;

    // ================= COMMON ================
    private String getLoginEmail() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        return principal.contains(":") ? principal.split(":")[0] : principal;
    }

    private void validateProductsInSameStore(List<Product> products, UUID storeId) {
        boolean ok = products.stream().allMatch(p -> p.getStore().getStoreId().equals(storeId));
        if (!ok) throw new RuntimeException("❌ tất cả sản phẩm phải thuộc 1 store");
    }

    private void validateActive(List<Product> products) {
        List<String> notActive = products.stream()
                .filter(p -> p.getStatus() != ProductStatus.ACTIVE)
                .map(Product::getName).toList();
        if (!notActive.isEmpty()) throw new RuntimeException("❌ sp không ACTIVE: " + String.join(", ", notActive));
    }

    private Map<UUID, Integer> buildQtyMap(List<ComboItemRequest> items) {
        if (items == null || items.isEmpty()) throw new RuntimeException("❌ items trống");
        Map<UUID, Integer> map = new HashMap<>();
        for (var i : items) {
            int q = i.getQuantity() == null ? 1 : i.getQuantity();
            if (q < 1) throw new RuntimeException("❌ quantity >=1");
            map.put(i.getProductId(), q);
        }
        return map;
    }

    @Override
    public ResponseEntity<BaseResponse> viewShopCombos(int page, int size, String keyword, Boolean isActive) {
        String email = getLoginEmail();
        Store store = storeRepository.findByAccount_Email(email).orElseThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductCombo> pageData =
                comboRepository.findByCreatorTypeAndCreatorId(ComboCreatorType.SHOP_CREATE, store.getStoreId(), pageable);

        List<ProductComboResponse> data = pageData.stream()
                .filter(c -> keyword == null || c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> isActive == null || Objects.equals(c.getIsActive(), isActive))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Combo shop của bạn", data));
    }

    @Override
    public ResponseEntity<BaseResponse> viewCustomerCombos(int page, int size, String keyword, Boolean isActive) {
        String email = getLoginEmail();
        Customer cus = customerRepository.findByAccount_Email(email).orElseThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductCombo> pageData =
                comboRepository.findByCreatorTypeAndCreatorId(ComboCreatorType.CUSTOMER_CREATE, cus.getId(), pageable);

        List<ProductComboResponse> data = pageData.stream()
                .filter(c -> keyword == null || c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> isActive == null || Objects.equals(c.getIsActive(), isActive))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Combo customer của bạn", data));
    }


    // =================== SHOP ===================

    @Override
    public ResponseEntity<BaseResponse> createShopCombo(CreateShopComboRequest req) {
        String email = getLoginEmail();
        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ store not found"));

        Map<UUID, Integer> qtyMap = buildQtyMap(req.getItems());
        List<Product> products = productRepository.findAllById(qtyMap.keySet());
        if (products.size() != qtyMap.size()) throw new RuntimeException("❌ productId not exist");

        validateProductsInSameStore(products, store.getStoreId());
        validateActive(products);

        ProductCombo combo = ProductCombo.builder()
                .store(store)
                .name(req.getName())
                .shortDescription(req.getShortDescription())
                .description(req.getDescription())
                .images(req.getImages())
                .videoUrl(req.getVideoUrl())
                .weight(req.getWeight())
                .stockQuantity(req.getStockQuantity())
                .shippingAddress(req.getShippingAddress())
                .warehouseLocation(req.getWarehouseLocation())
                .provinceCode(req.getProvinceCode())
                .districtCode(req.getDistrictCode())
                .wardCode(req.getWardCode())
                .creatorType(ComboCreatorType.SHOP_CREATE)
                .creatorId(store.getStoreId())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .createdBy(store.getAccount().getId())
                .build();

        combo.setItems(products.stream()
                .map(p -> ComboItem.builder().combo(combo).product(p).quantity(qtyMap.get(p.getProductId())).build())
                .toList());

        return ResponseEntity.ok(new BaseResponse<>(201, "✅ tạo combo shop", toResponse(comboRepository.save(combo))));
    }


    @Override
    public ResponseEntity<BaseResponse> updateShopCombo(UUID comboId, UpdateShopComboRequest req) {
        String email = getLoginEmail();
        Store store = storeRepository.findByAccount_Email(email).orElseThrow();

        ProductCombo combo = comboRepository.findById(comboId).orElseThrow();

        if (combo.getCreatorType() != ComboCreatorType.SHOP_CREATE || !combo.getCreatorId().equals(store.getStoreId()))
            throw new RuntimeException("❌ không có quyền update");

        applyCommonUpdate(combo, req);

        if (req.getItems() != null) {
            Map<UUID, Integer> qtyMap = buildQtyMap(req.getItems());
            List<Product> products = productRepository.findAllById(qtyMap.keySet());
            if (products.size() != qtyMap.size()) throw new RuntimeException("❌ productId not exist");
            validateProductsInSameStore(products, store.getStoreId());
            validateActive(products);
            combo.getItems().clear();
            products.forEach(p -> combo.getItems().add(
                    ComboItem.builder().combo(combo).product(p).quantity(qtyMap.get(p.getProductId())).build()
            ));
        }

        combo.setUpdatedAt(LocalDateTime.now());
        combo.setUpdatedBy(store.getAccount().getId());
        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ update combo shop", toResponse(comboRepository.save(combo))));
    }

    // =================== CUSTOMER ===================

    @Override
    public ResponseEntity<BaseResponse> createCustomerCombo(CreateCustomerComboRequest req) {
        String email = getLoginEmail();
        Customer cus = customerRepository.findByAccount_Email(email).orElseThrow();
        Store store = storeRepository.findById(req.getStoreId()).orElseThrow();

        Map<UUID, Integer> qtyMap = buildQtyMap(req.getItems());
        List<Product> products = productRepository.findAllById(qtyMap.keySet());
        if (products.size() != qtyMap.size()) throw new RuntimeException("❌ productId not exist");

        validateProductsInSameStore(products, store.getStoreId());
        validateActive(products);

        ProductCombo combo = ProductCombo.builder()
                .store(store)
                .name(req.getName())
                .shortDescription(req.getShortDescription())
                .description(req.getDescription())
                .images(req.getImages())
                .videoUrl(req.getVideoUrl())
                .weight(req.getWeight())
                .stockQuantity(req.getStockQuantity())
                .shippingAddress(req.getShippingAddress())
                .warehouseLocation(req.getWarehouseLocation())
                .provinceCode(req.getProvinceCode())
                .districtCode(req.getDistrictCode())
                .wardCode(req.getWardCode())
                .creatorType(ComboCreatorType.CUSTOMER_CREATE)
                .creatorId(cus.getId())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .createdBy(cus.getAccount().getId())
                .build();

        combo.setItems(products.stream()
                .map(p -> ComboItem.builder().combo(combo).product(p).quantity(qtyMap.get(p.getProductId())).build())
                .toList());

        return ResponseEntity.ok(new BaseResponse<>(201, "✅ tạo combo customer", toResponse(comboRepository.save(combo))));
    }


    @Override
    public ResponseEntity<BaseResponse> updateCustomerCombo(UUID comboId, UpdateCustomerComboRequest req) {
        String email = getLoginEmail();
        Customer cus = customerRepository.findByAccount_Email(email).orElseThrow();

        ProductCombo combo = comboRepository.findById(comboId).orElseThrow();
        if (combo.getCreatorType() != ComboCreatorType.CUSTOMER_CREATE || !combo.getCreatorId().equals(cus.getId()))
            throw new RuntimeException("❌ không có quyền update");

        applyCommonUpdate(combo, req);

        if (req.getItems() != null) {
            Map<UUID, Integer> qtyMap = buildQtyMap(req.getItems());
            List<Product> products = productRepository.findAllById(qtyMap.keySet());
            if (products.size() != qtyMap.size()) throw new RuntimeException("❌ productId not exist");
            validateProductsInSameStore(products, combo.getStore().getStoreId());
            validateActive(products);

            combo.getItems().clear();
            products.forEach(p -> combo.getItems().add(
                    ComboItem.builder().combo(combo).product(p).quantity(qtyMap.get(p.getProductId())).build()
            ));
        }

        combo.setUpdatedAt(LocalDateTime.now());
        combo.setUpdatedBy(cus.getAccount().getId());
        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ update combo customer", toResponse(comboRepository.save(combo))));
    }

    // ================== UPDATE HELPER SHOP ==================
    private void applyCommonUpdate(ProductCombo combo, UpdateShopComboRequest req) {
        if (req.getName() != null) combo.setName(req.getName());
        if (req.getShortDescription() != null) combo.setShortDescription(req.getShortDescription());
        if (req.getDescription() != null) combo.setDescription(req.getDescription());
        if (req.getImages() != null) combo.setImages(req.getImages());
        if (req.getVideoUrl() != null) combo.setVideoUrl(req.getVideoUrl());
        if (req.getWeight() != null) combo.setWeight(req.getWeight());
        if (req.getStockQuantity() != null) combo.setStockQuantity(req.getStockQuantity());
        if (req.getShippingAddress() != null) combo.setShippingAddress(req.getShippingAddress());
        if (req.getWarehouseLocation() != null) combo.setWarehouseLocation(req.getWarehouseLocation());

        if (req.getProvinceCode() != null) combo.setProvinceCode(req.getProvinceCode());
        if (req.getDistrictCode() != null) combo.setDistrictCode(req.getDistrictCode());
        if (req.getWardCode() != null) combo.setWardCode(req.getWardCode());

        if (req.getIsActive() != null) combo.setIsActive(req.getIsActive());
    }


    // ================== UPDATE HELPER CUSTOMER ==================
    private void applyCommonUpdate(ProductCombo combo, UpdateCustomerComboRequest req) {
        if (req.getName() != null) combo.setName(req.getName());
        if (req.getShortDescription() != null) combo.setShortDescription(req.getShortDescription());
        if (req.getDescription() != null) combo.setDescription(req.getDescription());
        if (req.getImages() != null) combo.setImages(req.getImages());
        if (req.getVideoUrl() != null) combo.setVideoUrl(req.getVideoUrl());
        if (req.getWeight() != null) combo.setWeight(req.getWeight());
        if (req.getStockQuantity() != null) combo.setStockQuantity(req.getStockQuantity());
        if (req.getShippingAddress() != null) combo.setShippingAddress(req.getShippingAddress());
        if (req.getWarehouseLocation() != null) combo.setWarehouseLocation(req.getWarehouseLocation());

        if (req.getProvinceCode() != null) combo.setProvinceCode(req.getProvinceCode());
        if (req.getDistrictCode() != null) combo.setDistrictCode(req.getDistrictCode());
        if (req.getWardCode() != null) combo.setWardCode(req.getWardCode());

        if (req.getIsActive() != null) combo.setIsActive(req.getIsActive());
    }

    private ProductComboResponse toResponse(ProductCombo c) {
        return ProductComboResponse.builder()
                .comboId(c.getComboId())
                .categoryName("COMBO")
                .name(c.getName())
                .shortDescription(c.getShortDescription())
                .description(c.getDescription())
                .images(c.getImages())
                .videoUrl(c.getVideoUrl())
                .provinceCode(c.getProvinceCode())
                .districtCode(c.getDistrictCode())
                .wardCode(c.getWardCode())
                .shippingAddress(c.getShippingAddress())
                .warehouseLocation(c.getWarehouseLocation())
                .stockQuantity(c.getStockQuantity())
                .storeId(c.getStore().getStoreId())        // <<<< ADD
                .storeName(c.getStore().getStoreName())
                .creatorType(c.getCreatorType().name())
                .creatorId(c.getCreatorId())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .items(c.getItems().stream().map(ci ->
                        ProductComboResponse.Item.builder()
                                .productId(ci.getProduct().getProductId())
                                .productName(ci.getProduct().getName())
                                .quantity(ci.getQuantity())
                                .build()
                ).toList())
                .build();
    }


}
