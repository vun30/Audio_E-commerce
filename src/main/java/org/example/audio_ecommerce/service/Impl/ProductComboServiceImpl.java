package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateComboRequest;
import org.example.audio_ecommerce.dto.request.UpdateComboRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ComboResponse;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.ProductCombo;
import org.example.audio_ecommerce.repository.ProductComboRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.service.ProductComboService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductComboServiceImpl implements ProductComboService {

    private final ProductComboRepository comboRepository;
    private final ProductRepository productRepository;

    // ✅ Tạo combo
    @Override
    public ResponseEntity<BaseResponse> createCombo(CreateComboRequest request) {
        Product comboProduct = productRepository.findById(request.getComboProductId())
                .orElseThrow(() -> new RuntimeException("Combo product not found"));

        List<Product> includedProducts = productRepository.findAllById(request.getIncludedProductIds());
        if (includedProducts.isEmpty()) {
            throw new RuntimeException("No included products found");
        }

        // ✅ Tính tổng giá lẻ
        BigDecimal totalPrice = includedProducts.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProductCombo combo = ProductCombo.builder()
                .comboProduct(comboProduct)
                .includedProducts(includedProducts)
                .store(comboProduct.getStore()) // ✅ Gán store tự động từ product
                .comboImageUrl(request.getComboImageUrl())
                .categoryName(request.getCategoryName())
                .categoryIconUrl(request.getCategoryIconUrl())
                .comboDescription(request.getComboDescription())
                .comboPrice(request.getComboPrice())
                .originalTotalPrice(totalPrice)
                .isActive(true)
                .build();

        comboRepository.save(combo);

        return ResponseEntity.ok(new BaseResponse<>(201, "Combo created successfully", buildResponse(combo)));
    }

    // ✅ Lấy combo theo ID
    @Override
    public ResponseEntity<BaseResponse> getComboById(UUID comboId) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo not found"));

        return ResponseEntity.ok(new BaseResponse<>(200, "Combo detail", buildResponse(combo)));
    }

    // ✅ Lấy tất cả combo (có lọc & phân trang)
    @Override
    public ResponseEntity<BaseResponse> getAllCombos(int page, int size, String keyword,
                                                     String sortDir, BigDecimal minPrice, BigDecimal maxPrice) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("desc") ?
                Sort.by("comboPrice").descending() :
                Sort.by("comboPrice").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductCombo> combos = comboRepository.findAll(pageable);

        // ✅ Lọc thêm trong memory
        List<ComboResponse> filtered = combos.stream()
                .filter(c -> keyword == null || c.getComboProduct().getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> minPrice == null || c.getComboPrice().compareTo(minPrice) >= 0)
                .filter(c -> maxPrice == null || c.getComboPrice().compareTo(maxPrice) <= 0)
                .map(this::buildResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new BaseResponse<>(200, "List of combos", filtered));
    }

    // ✅ Lấy combo theo storeId
    @Override
    public ResponseEntity<BaseResponse> getCombosByStoreId(UUID storeId, int page, int size, String keyword,
                                                           String sortDir, BigDecimal minPrice, BigDecimal maxPrice) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("desc") ?
                Sort.by("comboPrice").descending() :
                Sort.by("comboPrice").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductCombo> combos = comboRepository.findAll(pageable);

        List<ComboResponse> filtered = combos.stream()
                .filter(c -> c.getStore().getStoreId().equals(storeId))
                .filter(c -> keyword == null || c.getComboProduct().getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(c -> minPrice == null || c.getComboPrice().compareTo(minPrice) >= 0)
                .filter(c -> maxPrice == null || c.getComboPrice().compareTo(maxPrice) <= 0)
                .map(this::buildResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new BaseResponse<>(200, "Combos for store " + storeId, filtered));
    }

    // ✅ Cập nhật combo
    @Override
    public ResponseEntity<BaseResponse> updateCombo(UUID comboId, UpdateComboRequest request) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo not found"));

        if (request.getIncludedProductIds() != null) {
            List<Product> includedProducts = productRepository.findAllById(request.getIncludedProductIds());
            combo.setIncludedProducts(includedProducts);

            BigDecimal totalPrice = includedProducts.stream()
                    .map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            combo.setOriginalTotalPrice(totalPrice);
        }

        combo.setComboImageUrl(request.getComboImageUrl());
        combo.setCategoryName(request.getCategoryName());
        combo.setCategoryIconUrl(request.getCategoryIconUrl());
        combo.setComboDescription(request.getComboDescription());
        combo.setComboPrice(request.getComboPrice());
        combo.setIsActive(request.getIsActive());

        comboRepository.save(combo);

        return ResponseEntity.ok(new BaseResponse<>(200, "Combo updated successfully", buildResponse(combo)));
    }

    // ✅ Disable combo (thay cho delete)
    @Override
    public ResponseEntity<BaseResponse> disableCombo(UUID comboId) {
        ProductCombo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo not found"));

        combo.setIsActive(false);
        comboRepository.save(combo);

        return ResponseEntity.ok(new BaseResponse<>(200, "Combo disabled successfully", buildResponse(combo)));
    }

    // ✅ Build response
    private ComboResponse buildResponse(ProductCombo combo) {
        return ComboResponse.builder()
                .comboId(combo.getComboId())
                .comboProductId(combo.getComboProduct().getProductId())
                .comboName(combo.getComboProduct().getName())
                .comboImageUrl(combo.getComboImageUrl())
                .categoryName(combo.getCategoryName())
                .categoryIconUrl(combo.getCategoryIconUrl())
                .comboDescription(combo.getComboDescription())
                .comboPrice(combo.getComboPrice())
                .originalTotalPrice(combo.getOriginalTotalPrice())
                .isActive(combo.getIsActive())
                .includedProductIds(combo.getIncludedProducts()
                        .stream()
                        .map(Product::getProductId)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
public ResponseEntity<BaseResponse> getProductsInCombo(UUID comboId) {
    ProductCombo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new RuntimeException("Combo not found"));

    // ✅ Lấy danh sách sản phẩm thuộc combo
    List<Product> products = combo.getIncludedProducts();

    // ✅ Map ra response nếu cần đơn giản hơn (chỉ trả ID, tên, giá,…)
    List<Object> productResponses = products.stream().map(p -> {
        return new java.util.HashMap<String, Object>() {{
            put("productId", p.getProductId());
            put("name", p.getName());
            put("price", p.getPrice());
            put("category", p.getCategory().getName());
            put("image", p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null);
            put("status", p.getStatus());
        }};
    }).collect(Collectors.toList());

    return ResponseEntity.ok(new BaseResponse<>(200, "List of products in combo " + comboId, productResponses));
}
}
