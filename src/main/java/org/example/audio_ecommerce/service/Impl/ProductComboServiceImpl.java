package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ComboResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.example.audio_ecommerce.repository.ProductComboRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductComboServiceImpl implements ProductComboService {

    private final ProductComboRepository comboRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    /**
     * ✅ Tạo combo mới
     */
    @Override
    public ResponseEntity<BaseResponse> createCombo(CreateComboRequest request) {
        // 1️⃣ Lấy danh sách sản phẩm được chọn
        List<Product> includedProducts = productRepository.findAllById(request.getIncludedProductIds());
        if (includedProducts.isEmpty()) {
            throw new RuntimeException("❌ Không tìm thấy sản phẩm trong danh sách đã chọn");
        }

        // 2️⃣ Kiểm tra store tồn tại
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Store không tồn tại"));

        // 3️⃣ Kiểm tra tất cả sản phẩm có thuộc cùng store hay không
        boolean sameStore = includedProducts.stream()
                .allMatch(p -> p.getStore().getStoreId().equals(store.getStoreId()));
        if (!sameStore) {
            throw new RuntimeException("❌ Tất cả sản phẩm trong combo phải thuộc cùng một cửa hàng");
        }

        // 4️⃣ Kiểm tra tất cả sản phẩm có đang ở trạng thái ACTIVE hay không
        List<Product> inactiveProducts = includedProducts.stream()
                .filter(p -> p.getStatus() != ProductStatus.ACTIVE)
                .collect(Collectors.toList());
        if (!inactiveProducts.isEmpty()) {
            String productNames = inactiveProducts.stream()
                    .map(Product::getName)
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("❌ Không thể tạo combo. Các sản phẩm sau không ở trạng thái ACTIVE: " + productNames);
        }

        // 5️⃣ Kiểm tra category tồn tại
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("❌ Danh mục không tồn tại"));
        }

        // 6️⃣ Tính tổng giá gốc nếu mua lẻ
        BigDecimal totalPrice = includedProducts.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7️⃣ Tạo combo mới
        ProductCombo combo = ProductCombo.builder()
                .store(store)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .images(request.getImages())
                .videoUrl(request.getVideoUrl())
                .weight(request.getWeight())
                .stockQuantity(request.getStockQuantity())
                .shippingAddress(request.getShippingAddress())
                .warehouseLocation(request.getWarehouseLocation())
                .comboPrice(request.getComboPrice())
                .originalTotalPrice(totalPrice)
                .includedProducts(includedProducts)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        comboRepository.save(combo);

        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Combo tạo thành công", buildResponse(combo, category)));
    }

    /**
     * 🔎 Lấy chi tiết combo theo ID
     */
    @Override
    public ResponseEntity<BaseResponse> getComboById(UUID comboId) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("❌ Combo không tồn tại"));
        Category category = combo.getCategoryId() != null ? categoryRepository.findById(combo.getCategoryId()).orElse(null) : null;
        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Chi tiết combo", buildResponse(combo, category)));
    }

    /**
     * 📦 Lấy tất cả combo
     */
    @Override
    public ResponseEntity<BaseResponse> getAllCombos(int page, int size, String keyword,
                                                     String sortDir, BigDecimal minPrice, BigDecimal maxPrice ,  Boolean isActive) {
        Sort sort = (sortDir != null && sortDir.equalsIgnoreCase("desc"))
                ? Sort.by("comboPrice").descending()
                : Sort.by("comboPrice").ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductCombo> combos = comboRepository.findAll(pageable);

        List<ComboResponse> filtered = combos.stream()
                .filter(c -> keyword == null || c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> minPrice == null || c.getComboPrice().compareTo(minPrice) >= 0)
                .filter(c -> maxPrice == null || c.getComboPrice().compareTo(maxPrice) <= 0)
                 .filter(c -> isActive == null || c.getIsActive().equals(isActive))
                .map(c -> buildResponse(c, c.getCategoryId() != null ? categoryRepository.findById(c.getCategoryId()).orElse(null) : null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Danh sách combo", filtered));
    }

    /**
     * 🏪 Lấy danh sách combo theo Store ID
     */
    @Override
    public ResponseEntity<BaseResponse> getCombosByStoreId(UUID storeId, int page, int size,
                                                           String keyword, String sortDir,
                                                           BigDecimal minPrice, BigDecimal maxPrice) {
        Sort sort = (sortDir != null && sortDir.equalsIgnoreCase("desc"))
                ? Sort.by("comboPrice").descending()
                : Sort.by("comboPrice").ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductCombo> combos = comboRepository.findAll(pageable);

        List<ComboResponse> filtered = combos.stream()
                .filter(c -> c.getStore().getStoreId().equals(storeId))
                .filter(c -> keyword == null || c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> minPrice == null || c.getComboPrice().compareTo(minPrice) >= 0)
                .filter(c -> maxPrice == null || c.getComboPrice().compareTo(maxPrice) <= 0)
                .map(c -> buildResponse(c, c.getCategoryId() != null ? categoryRepository.findById(c.getCategoryId()).orElse(null) : null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Combo của cửa hàng " + storeId, filtered));
    }

    /**
     * ✏️ Cập nhật combo
     */
    @Override
    public ResponseEntity<BaseResponse> updateCombo(UUID comboId, UpdateComboRequest request) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("❌ Combo không tồn tại"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("❌ Danh mục không tồn tại"));
            combo.setCategoryId(category.getCategoryId());
        } else if (combo.getCategoryId() != null) {
            category = categoryRepository.findById(combo.getCategoryId()).orElse(null);
        }

        if (request.getName() != null) combo.setName(request.getName());
        if (request.getShortDescription() != null) combo.setShortDescription(request.getShortDescription());
        if (request.getDescription() != null) combo.setDescription(request.getDescription());
        if (request.getImages() != null) combo.setImages(request.getImages());
        if (request.getVideoUrl() != null) combo.setVideoUrl(request.getVideoUrl());
        if (request.getWeight() != null) combo.setWeight(request.getWeight());
        if (request.getStockQuantity() != null) combo.setStockQuantity(request.getStockQuantity());
        if (request.getShippingAddress() != null) combo.setShippingAddress(request.getShippingAddress());
        if (request.getWarehouseLocation() != null) combo.setWarehouseLocation(request.getWarehouseLocation());
        if (request.getComboPrice() != null) combo.setComboPrice(request.getComboPrice());
        if (request.getIsActive() != null) combo.setIsActive(request.getIsActive());

        if (request.getIncludedProductIds() != null) {
            List<Product> products = productRepository.findAllById(request.getIncludedProductIds());

            // ✅ Kiểm tra trạng thái sản phẩm khi cập nhật combo
            List<Product> inactiveProducts = products.stream()
                    .filter(p -> p.getStatus() != ProductStatus.ACTIVE)
                    .collect(Collectors.toList());
            if (!inactiveProducts.isEmpty()) {
                String productNames = inactiveProducts.stream()
                        .map(Product::getName)
                        .collect(Collectors.joining(", "));
                throw new RuntimeException("❌ Không thể cập nhật combo. Các sản phẩm sau không ở trạng thái ACTIVE: " + productNames);
            }

            combo.setIncludedProducts(products);
            combo.setOriginalTotalPrice(products.stream()
                    .map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        combo.setUpdatedAt(LocalDateTime.now());
        comboRepository.save(combo);

        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Combo cập nhật thành công", buildResponse(combo, category)));
    }

    /**
     * 🛑 Disable combo
     */
    @Override
    public ResponseEntity<BaseResponse> disableCombo(UUID comboId) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("❌ Combo không tồn tại"));

        combo.setIsActive(false);
        combo.setUpdatedAt(LocalDateTime.now());
        comboRepository.save(combo);

        Category category = combo.getCategoryId() != null ? categoryRepository.findById(combo.getCategoryId()).orElse(null) : null;
        return ResponseEntity.ok(new BaseResponse<>(200, "🛑 Combo đã bị vô hiệu hóa", buildResponse(combo, category)));
    }

    /**
     * 📦 Lấy danh sách sản phẩm con
     */
    @Override
    public ResponseEntity<BaseResponse> getProductsInCombo(UUID comboId) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("❌ Combo không tồn tại"));
        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Danh sách sản phẩm trong combo", combo.getIncludedProducts()));
    }

    /**
     * 📤 Build DTO từ entity
     */
    private ComboResponse buildResponse(ProductCombo combo, Category category) {
        return ComboResponse.builder()
                .comboId(combo.getComboId())
                .storeId(combo.getStore().getStoreId())
                .storeName(combo.getStore().getStoreName())
                .categoryId(combo.getCategoryId())
                .categoryName(category != null ? category.getName() : null)
                .name(combo.getName())
                .shortDescription(combo.getShortDescription())
                .description(combo.getDescription())
                .images(combo.getImages())
                .videoUrl(combo.getVideoUrl())
                .weight(combo.getWeight())
                .stockQuantity(combo.getStockQuantity())
                .shippingAddress(combo.getShippingAddress())
                .warehouseLocation(combo.getWarehouseLocation())
                .comboPrice(combo.getComboPrice())
                .originalTotalPrice(combo.getOriginalTotalPrice())
                .isActive(combo.getIsActive())
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .includedProductIds(combo.getIncludedProducts().stream().map(Product::getProductId).collect(Collectors.toList()))
                .includedProductNames(combo.getIncludedProducts().stream().map(Product::getName).collect(Collectors.toList()))
                .build();
    }
}
