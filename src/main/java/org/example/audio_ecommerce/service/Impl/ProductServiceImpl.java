// ==========================
// PRODUCT SERVICE IMPLEMENT
// ==========================
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductVariantRepository productVariantRepository;

    // ================================================
    // SLUG HELPER
    // ================================================
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

    // ================================================
    // TOTAL STOCK FROM VARIANTS
    // ================================================
    private int calculateVariantStockTotal(UUID productId) {
        return productVariantRepository.findAllByProduct_ProductId(productId)
                .stream()
                .mapToInt(ProductVariantEntity::getVariantStock)
                .sum();
    }

    // ================================================
    // CREATE PRODUCT
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> createProduct(ProductRequest req) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = principal.contains(":") ? principal.split(":")[0] : principal;

            Store store = storeRepository.findByAccount_Email(email)
                    .orElseThrow(() -> new RuntimeException("❌ Store not found for logged-in account"));

            if (req.getCategoryIds() == null || req.getCategoryIds().isEmpty()) {
                throw new RuntimeException("❌ categoryIds cannot be empty");
            }

            List<Category> categories = categoryRepository.findAllById(req.getCategoryIds());
            if (categories.size() != req.getCategoryIds().size()) {
                throw new RuntimeException("❌ Some categoryIds are invalid");
            }

            if (req.getSku() == null || req.getSku().isBlank())
                throw new RuntimeException("❌ SKU must not be empty");

            if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku())) {
                throw new RuntimeException("❌ SKU already exists in this store");
            }

            LocalDateTime now = LocalDateTime.now();

            Product p = new Product();
            p.setStore(store);
            p.setCategories(categories);
            p.setBrandName(req.getBrandName());
            p.setName(req.getName());
            p.setSlug(generateUniqueSlug(req.getName()));
            p.setSku(req.getSku());
            p.setStatus(ProductStatus.DRAFT);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            p.setLastUpdatedAt(now);
            p.setLastUpdateIntervalDays(0L);
            p.setCreatedBy(store.getAccount().getId());
            p.setUpdatedBy(store.getAccount().getId());
            // FORCE PRODUCT TO BACK TO DRAFT WHEN UPDATE


            // BASIC FIELDS
            p.setShortDescription(req.getShortDescription());
            p.setDescription(req.getDescription());
            p.setModel(req.getModel());
            p.setColor(req.getColor());
            p.setMaterial(req.getMaterial());
            p.setDimensions(req.getDimensions());
            p.setWeight(req.getWeight());
            p.setImages(req.getImages());
            p.setVideoUrl(req.getVideoUrl());
            p.setWarehouseLocation(req.getWarehouseLocation());
            p.setShippingAddress(req.getShippingAddress());
            p.setProvinceCode(req.getProvinceCode());
            p.setDistrictCode(req.getDistrictCode());
            p.setWardCode(req.getWardCode());
            p.setShippingFee(req.getShippingFee());
            p.setSupportedShippingMethodIds(req.getSupportedShippingMethodIds());

            boolean hasVariants = req.getVariants() != null && !req.getVariants().isEmpty();

            if (!hasVariants) {
                if (req.getPrice() == null)
                    throw new RuntimeException("❌ price must not be null when product has NO variants");
                p.setPrice(req.getPrice());
                p.setFinalPrice(req.getPrice());
            } else {
                p.setPrice(null);
                p.setFinalPrice(null);
            }

            productRepository.save(p);

            // ATTRIBUTE VALUES
            if (req.getAttributeValues() != null) {
                for (ProductAttributeValueRequest a : req.getAttributeValues()) {
                    CategoryAttribute attr = categoryAttributeRepository.findById(a.getAttributeId())
                            .orElseThrow(() -> new RuntimeException("❌ Attribute not found"));

                    ProductAttributeValue pav = new ProductAttributeValue();
                    pav.setProduct(p);
                    pav.setAttribute(attr);
                    pav.setValue(a.getValue());
                    productAttributeValueRepository.save(pav);
                }
            }

            // VARIANTS
            if (hasVariants) {
                for (VariantRequest v : req.getVariants()) {
                    ProductVariantEntity variant = new ProductVariantEntity();
                    variant.setProduct(p);
                    variant.setOptionName(v.getOptionName());
                    variant.setOptionValue(v.getOptionValue());
                    variant.setVariantPrice(v.getVariantPrice());
                    variant.setVariantStock(v.getVariantStock());
                    variant.setVariantUrl(v.getVariantUrl());
                    variant.setVariantSku(v.getVariantSku());
                    productVariantRepository.save(variant);
                }

                p.setStockQuantity(calculateVariantStockTotal(p.getProductId()));
                productRepository.save(p);
            }

            return ResponseEntity.ok(
                    new BaseResponse<>(201, "✅ Product created successfully", p.getProductId())
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ Create product failed: " + e.getMessage()));
        }
    }

    // ================================================
    // UPDATE PRODUCT
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> updateProduct(UUID id, UpdateProductRequest req) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = principal.contains(":") ? principal.split(":")[0] : principal;

            Store store = storeRepository.findByAccount_Email(email)
                    .orElseThrow(() -> new RuntimeException("❌ Store not found"));

            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            if (!p.getStore().getStoreId().equals(store.getStoreId())) {
                throw new RuntimeException("❌ You cannot update product from another store");
            }

            // UPDATE CATEGORY
            if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
                List<Category> categories = categoryRepository.findAllById(req.getCategoryIds());
                if (categories.size() != req.getCategoryIds().size()) {
                    throw new RuntimeException("❌ Some categoryIds are invalid");
                }
                p.setCategories(categories);
            }

            // BASIC UPDATE
            if (req.getName() != null) p.setName(req.getName());
            if (req.getSlug() != null) p.setSlug(req.getSlug());
            if (req.getBrandName() != null) p.setBrandName(req.getBrandName());
            if (req.getShortDescription() != null) p.setShortDescription(req.getShortDescription());
            if (req.getDescription() != null) p.setDescription(req.getDescription());
            if (req.getModel() != null) p.setModel(req.getModel());
            if (req.getColor() != null) p.setColor(req.getColor());
            if (req.getMaterial() != null) p.setMaterial(req.getMaterial());
            if (req.getDimensions() != null) p.setDimensions(req.getDimensions());
            if (req.getWeight() != null) p.setWeight(req.getWeight());
            if (req.getImages() != null) p.setImages(req.getImages());
            if (req.getVideoUrl() != null) p.setVideoUrl(req.getVideoUrl());
            if (req.getWarehouseLocation() != null) p.setWarehouseLocation(req.getWarehouseLocation());
            if (req.getShippingAddress() != null) p.setShippingAddress(req.getShippingAddress());
            if (req.getProvinceCode() != null) p.setProvinceCode(req.getProvinceCode());
            if (req.getDistrictCode() != null) p.setDistrictCode(req.getDistrictCode());
            if (req.getWardCode() != null) p.setWardCode(req.getWardCode());
            if (req.getShippingFee() != null) p.setShippingFee(req.getShippingFee());
            if (req.getSupportedShippingMethodIds() != null)
                p.setSupportedShippingMethodIds(req.getSupportedShippingMethodIds());

            // SKU CHECK
            if (req.getSku() != null && !req.getSku().equals(p.getSku())) {
                if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku())) {
                    throw new RuntimeException("❌ SKU already exists in this store");
                }
                p.setSku(req.getSku());
            }

            // UPDATE TIMESTAMP
            LocalDateTime now = LocalDateTime.now();
            long interval = ChronoUnit.DAYS.between(p.getLastUpdatedAt(), now);
            p.setLastUpdateIntervalDays(interval);
            p.setLastUpdatedAt(now);
            p.setUpdatedAt(now);
            p.setUpdatedBy(store.getAccount().getId());
            p.setStatus(ProductStatus.DRAFT);

            // UPDATE ATTRIBUTE VALUES
            if (req.getAttributeValues() != null) {
                productAttributeValueRepository.deleteAll(
                        productAttributeValueRepository.findAllByProduct_ProductId(id)
                );

                for (ProductAttributeValueRequest a : req.getAttributeValues()) {
                    CategoryAttribute attr = categoryAttributeRepository.findById(a.getAttributeId())
                            .orElseThrow(() -> new RuntimeException("❌ Attribute not found"));

                    ProductAttributeValue pav = new ProductAttributeValue();
                    pav.setProduct(p);
                    pav.setAttribute(attr);
                    pav.setValue(a.getValue());
                    productAttributeValueRepository.save(pav);
                }
            }

            // VARIANT LOGIC
            boolean hasExistingVariants =
                    productVariantRepository.countByProduct_ProductId(id) > 0;

            boolean hasIncomingVariants =
                    (req.getVariantsToAdd() != null && !req.getVariantsToAdd().isEmpty())
                            || (req.getVariantsToUpdate() != null && !req.getVariantsToUpdate().isEmpty());

            boolean finalHasVariants = hasExistingVariants || hasIncomingVariants;

            if (finalHasVariants) {
                p.setPrice(null);
                p.setFinalPrice(null);
            } else if (req.getPrice() != null) {
                p.setPrice(req.getPrice());
                p.setFinalPrice(req.getPrice());
            }

            // DELETE VARIANTS
            if (req.getVariantsToDelete() != null) {
                for (UUID vid : req.getVariantsToDelete()) {
                    if (!productVariantRepository.existsByIdAndProduct_ProductId(vid, id)) {
                        throw new RuntimeException("❌ Variant ID not belongs to product");
                    }
                    productVariantRepository.deleteById(vid);
                }
            }

            // UPDATE VARIANTS
            if (req.getVariantsToUpdate() != null) {
                for (UpdateProductRequest.VariantToUpdate v : req.getVariantsToUpdate()) {
                    ProductVariantEntity variant = productVariantRepository
                            .findByIdAndProduct_ProductId(v.getVariantId(), id)
                            .orElseThrow(() -> new RuntimeException("❌ Variant not found"));

                    if (v.getOptionName() != null) variant.setOptionName(v.getOptionName());
                    if (v.getOptionValue() != null) variant.setOptionValue(v.getOptionValue());
                    if (v.getVariantPrice() != null) variant.setVariantPrice(v.getVariantPrice());
                    if (v.getVariantStock() != null) variant.setVariantStock(v.getVariantStock());
                    if (v.getVariantUrl() != null) variant.setVariantUrl(v.getVariantUrl());
                    if (v.getVariantSku() != null) variant.setVariantSku(v.getVariantSku());

                    productVariantRepository.save(variant);
                }
            }

            // ADD VARIANTS
            if (req.getVariantsToAdd() != null) {
                for (UpdateProductRequest.VariantToAdd v : req.getVariantsToAdd()) {
                    ProductVariantEntity newV = new ProductVariantEntity();
                    newV.setProduct(p);
                    newV.setOptionName(v.getOptionName());
                    newV.setOptionValue(v.getOptionValue());
                    newV.setVariantPrice(v.getVariantPrice());
                    newV.setVariantStock(v.getVariantStock());
                    newV.setVariantUrl(v.getVariantUrl());
                    newV.setVariantSku(v.getVariantSku());
                    productVariantRepository.save(newV);
                }
            }

            // SYNC STOCK
            List<ProductVariantEntity> finalVariants =
                    productVariantRepository.findAllByProduct_ProductId(id);

            if (!finalVariants.isEmpty()) {
                int totalStock = finalVariants.stream()
                        .mapToInt(ProductVariantEntity::getVariantStock)
                        .sum();

                p.setStockQuantity(totalStock);
            } else {
                if (req.getStockQuantity() != null) {
                    p.setStockQuantity(req.getStockQuantity());
                }
            }

            productRepository.save(p);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "✏️ Product updated successfully", id)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ Update product failed: " + e.getMessage()));
        }
    }

    // ================================================
    // CONVERT TO RESPONSE
    // ================================================
    private ProductResponse toResponse(Product p) {

        List<ProductVariantEntity> variants =
                productVariantRepository.findAllByProduct_ProductId(p.getProductId());

        List<ProductAttributeValue> attributeValues =
                productAttributeValueRepository.findAllByProduct_ProductId(p.getProductId());

        return ProductResponse.builder()
                .productId(p.getProductId())
                .storeId(p.getStore().getStoreId())
                .storeName(p.getStore().getStoreName())

                .categories(
                        p.getCategories().stream()
                                .map(c -> new ProductResponse.CategoryResponse(
                                        c.getCategoryId(),
                                        c.getName()
                                ))
                                .toList()
                )

                .brandName(p.getBrandName())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .model(p.getModel())
                .color(p.getColor())
                .material(p.getMaterial())
                .dimensions(p.getDimensions())
                .weight(p.getWeight())
                .images(p.getImages())
                .videoUrl(p.getVideoUrl())
                .sku(p.getSku())
                .currency(p.getCurrency())
                .warehouseLocation(p.getWarehouseLocation())
                .shippingAddress(p.getShippingAddress())
                .provinceCode(p.getProvinceCode())
                .districtCode(p.getDistrictCode())
                .wardCode(p.getWardCode())
                .shippingFee(p.getShippingFee())
                .supportedShippingMethodIds(p.getSupportedShippingMethodIds())
                .status(p.getStatus())
                .approvalReason(p.getApprovalReason())

                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .promotionPercent(p.getPromotionPercent())
                .priceAfterPromotion(p.getPriceAfterPromotion())
                .priceBeforeVoucher(p.getPriceBeforeVoucher())
                .voucherAmount(p.getVoucherAmount())
                .finalPrice(p.getFinalPrice())
                .platformFeePercent(p.getPlatformFeePercent())
                .stockQuantity(p.getStockQuantity())

                .variants(
                        variants.stream()
                                .map(v -> new ProductResponse.VariantResponse(
                                        v.getId(),
                                        v.getOptionName(),
                                        v.getOptionValue(),
                                        v.getVariantPrice(),
                                        v.getVariantStock(),
                                        v.getVariantUrl(),
                                        v.getVariantSku()
                                ))
                                .toList()
                )

                .attributeValues(
                        attributeValues.stream()
                                .map(a -> new ProductResponse.ProductAttributeValueResponse(
                                        a.getAttribute().getAttributeId(),
                                        a.getAttribute().getAttributeName(),
                                        a.getAttribute().getAttributeLabel(),
                                        a.getAttribute().getDataType(),
                                        a.getValue()
                                ))
                                .toList()
                )


                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .lastUpdatedAt(p.getLastUpdatedAt())
                .lastUpdateIntervalDays(p.getLastUpdateIntervalDays())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    // ================================================
    // GET PRODUCT BY ID
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> getProductById(UUID id) {
        try {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "🔎 Product detail", toResponse(p))
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ getProductById failed: " + e.getMessage()));
        }
    }

    // ================================================
    // 🔥🔥🔥 CLEAN VERSION — GET ALL PRODUCTS (NO DUPLICATE)
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> getAllProducts(
            String categoryName,
            UUID storeId,
            String keyword,
            int page,
            int size,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            Page<Product> products = productRepository.findAll(pageable);

            String normalizedCategory = (categoryName != null && !categoryName.isBlank())
                    ? categoryName.trim().toLowerCase()
                    : null;

            List<ProductResponse> filtered = products.getContent().stream()

                    // CATEGORY
                    .filter(p -> {
                        if (normalizedCategory == null) return true;
                        Set<String> cateNames = p.getCategories().stream()
                                .map(c -> c.getName().toLowerCase())
                                .collect(Collectors.toSet());
                        return cateNames.contains(normalizedCategory);
                    })

                    // STORE
                    .filter(p -> storeId == null || p.getStore().getStoreId().equals(storeId))

                    // KEYWORD
                    .filter(p -> keyword == null ||
                            (p.getName() != null &&
                                    p.getName().toLowerCase().contains(keyword.toLowerCase())))

                    // STATUS
                    .filter(p -> status == null || p.getStatus() == status)

                    // PRICE
                    .filter(p -> {
                        BigDecimal lowestPrice = p.getPrice();

                        List<ProductVariantEntity> variants =
                                productVariantRepository.findAllByProduct_ProductId(p.getProductId());

                        if (!variants.isEmpty()) {
                            lowestPrice = variants.stream()
                                    .map(ProductVariantEntity::getVariantPrice)
                                    .filter(Objects::nonNull)
                                    .min(BigDecimal::compareTo)
                                    .orElse(lowestPrice);
                        }

                        if (minPrice != null && lowestPrice.compareTo(minPrice) < 0) return false;
                        if (maxPrice != null && lowestPrice.compareTo(maxPrice) > 0) return false;

                        return true;
                    })

                    .map(this::toResponse)
                    .toList();

            return ResponseEntity.ok(
                    BaseResponse.success("📦 Product list filtered successfully", filtered)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ getAllProducts failed: " + e.getMessage()));
        }
    }

    // ================================================
    // DISABLE PRODUCT
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> disableProduct(UUID productId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            if (product.getStatus() == ProductStatus.INACTIVE) {
                product.setStatus(ProductStatus.ACTIVE);
            } else {
                product.setStatus(ProductStatus.INACTIVE);
            }

            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "🚫 Product status updated", toResponse(product))
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ disableProduct failed: " + e.getMessage()));
        }
    }

    // ================================================
    // INCREMENT VIEW
    // ================================================
    @Override
    public ResponseEntity<BaseResponse> incrementViewCount(UUID productId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            int current = product.getViewCount() != null ? product.getViewCount() : 0;
            product.setViewCount(current + 1);

            productRepository.save(product);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "👁️ View count incremented",
                            Map.of("productId", productId, "viewCount", product.getViewCount()))
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("❌ incrementViewCount failed: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<BaseResponse> approveProduct(UUID productId, ApproveProductRequest req) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            // Chỉ duyệt được DRAFT
            if (product.getStatus() != ProductStatus.DRAFT) {
                throw new RuntimeException("❌ Only DRAFT products can be reviewed");
            }

            // TỪ CHỐI → yêu cầu nhập lý do
            if (!req.isApproved() && (req.getReason() == null || req.getReason().trim().isEmpty())) {
                throw new RuntimeException("❌ Reason is required when rejecting");
            }

            // APPROVE
            if (req.isApproved()) {
                product.setStatus(ProductStatus.ACTIVE);
                product.setApprovalReason(req.getReason());
            }
            // REJECT
            else {
                product.setStatus(ProductStatus.DRAFT);  // giữ nguyên
                product.setApprovalReason(req.getReason());
            }

            productRepository.save(product);

            return ResponseEntity.ok(
                    BaseResponse.success("✔ Product review updated", Map.of(
                            "productId", productId,
                            "approved", req.isApproved(),
                            "status", product.getStatus(),
                            "reason", product.getApprovalReason()
                    ))
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    BaseResponse.error("❌ approveProduct failed: " + e.getMessage())
            );
        }
    }


}
