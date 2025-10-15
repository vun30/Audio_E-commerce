package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ProductResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.ProductService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    // ============================================================
    // 🔧 Helper: Sinh slug duy nhất
    // ============================================================
    private String generateUniqueSlug(String productName) {
        if (productName == null || productName.isBlank())
            return UUID.randomUUID().toString().substring(0, 8);

        String base = productName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    // ============================================================
    // ➕ CREATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> createProduct(ProductRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for logged-in account"));

        // 🔍 Tìm Category theo tên (categoryName)
        if (req.getCategoryName() == null || req.getCategoryName().isBlank())
            throw new RuntimeException("❌ Category Name must not be null");

        Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));

        // SKU check
        if (req.getSku() == null || req.getSku().isBlank())
            throw new RuntimeException("❌ SKU must not be empty");

        if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
            throw new RuntimeException("❌ SKU already exists in this store");

        // ✅ Tạo Product
        Product p = new Product();
        p.setStore(store);
        p.setCategory(category);
        p.setBrandName(req.getBrandName());
        p.setName(req.getName());
        p.setSlug(generateUniqueSlug(req.getName()));
        p.setSku(req.getSku());
        applyRequestToProduct(p, req);
        p.setStatus(ProductStatus.ACTIVE);
        p.setIsFeatured(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setCreatedBy(store.getAccount().getId());

        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Product created successfully", toResponse(p)));
    }

    // ============================================================
    // ✏️ UPDATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> updateProduct(UUID id, ProductRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found"));

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));

        if (!p.getStore().getStoreId().equals(store.getStoreId()))
            throw new RuntimeException("❌ Cannot update another store's product");

        // Nếu đổi categoryName → tìm category mới
        if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
            Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                    .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));
            p.setCategory(category);
        }

        if (req.getName() != null) {
            p.setName(req.getName());
            p.setSlug(generateUniqueSlug(req.getName()));
        }

        if (req.getSku() != null && !req.getSku().equals(p.getSku())) {
            if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
                throw new RuntimeException("❌ SKU already exists in this store");
            p.setSku(req.getSku());
        }

        applyRequestToProduct(p, req);

        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(store.getAccount().getId());

        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Product updated successfully", toResponse(p)));
    }

    // ============================================================
    // 🔄 ENABLE / DISABLE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> disableProduct(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));
        p.setStatus(p.getStatus() == ProductStatus.ACTIVE ? ProductStatus.DISCONTINUED : ProductStatus.ACTIVE);
        p.setUpdatedAt(LocalDateTime.now());
        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(200, "🔄 Product status changed", toResponse(p)));
    }

    // ============================================================
    // 📜 GET ALL / 🔍 BY ID
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getAllProducts(
            UUID categoryId, UUID storeId, String keyword, int page, int size, ProductStatus status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findAll(pageable);

        List<ProductResponse> filtered = products.stream()
                .filter(p -> categoryId == null || p.getCategory().getCategoryId().equals(categoryId))
                .filter(p -> storeId == null || p.getStore().getStoreId().equals(storeId))
                .filter(p -> keyword == null || p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> status == null || p.getStatus() == status)
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Product list", filtered));
    }

    @Override
    public ResponseEntity<BaseResponse> getProductById(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));
        return ResponseEntity.ok(new BaseResponse<>(200, "🔎 Product detail", toResponse(p)));
    }

    // ============================================================
    // 🧩 Convert Entity → Response
    // ============================================================
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .productId(p.getProductId())
                .storeId(p.getStore().getStoreId())
                .storeName(p.getStore().getStoreName())
                .categoryId(p.getCategory().getCategoryId())
                .categoryName(p.getCategory().getName())
                .brandName(p.getBrandName())
                .name(p.getName())
                .slug(p.getSlug())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .currency(p.getCurrency())
                .stockQuantity(p.getStockQuantity())
                .images(p.getImages())
                .videoUrl(p.getVideoUrl())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private void applyRequestToProduct(Product p, ProductRequest r) {
        if (r.getBrandName() != null) p.setBrandName(r.getBrandName());
        if (r.getShortDescription() != null) p.setShortDescription(r.getShortDescription());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getModel() != null) p.setModel(r.getModel());
        if (r.getColor() != null) p.setColor(r.getColor());
        if (r.getMaterial() != null) p.setMaterial(r.getMaterial());
        if (r.getDimensions() != null) p.setDimensions(r.getDimensions());
        if (r.getWeight() != null) p.setWeight(r.getWeight());
        if (r.getImages() != null) p.setImages(r.getImages());
        if (r.getVideoUrl() != null) p.setVideoUrl(r.getVideoUrl());
        if (r.getPrice() != null) p.setPrice(r.getPrice());
        if (r.getDiscountPrice() != null) p.setDiscountPrice(r.getDiscountPrice());
        if (r.getCurrency() != null) p.setCurrency(r.getCurrency());
        if (r.getStockQuantity() != null) p.setStockQuantity(r.getStockQuantity());
        if (r.getWarehouseLocation() != null) p.setWarehouseLocation(r.getWarehouseLocation());
        if (r.getShippingAddress() != null) p.setShippingAddress(r.getShippingAddress());
    }
}
