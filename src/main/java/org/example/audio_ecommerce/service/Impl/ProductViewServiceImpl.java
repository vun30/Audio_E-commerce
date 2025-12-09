package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherProductRepository;
import org.example.audio_ecommerce.service.ProductViewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductViewServiceImpl implements ProductViewService {

    private final ProductRepository productRepo;
    private final ShopVoucherProductRepository shopVoucherProductRepo;
    private final PlatformCampaignProductRepository platformCampaignProductRepo;

    // =========================================================
    // 1) LIST THUMBNAIL VIEW + FILTER
    // =========================================================
    @Override
    public ResponseEntity<BaseResponse> getThumbnailView(
        String status,
        UUID categoryId,
        UUID storeId,
        String keyword,
        String provinceCode,
        String districtCode,
        String wardCode,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        Pageable pageable,
        String sortBy,      // NEW
        String sortDir      // NEW
)
 {

        Page<Product> products = productRepo.findAllWithAdvancedFilters(
                status, categoryId, storeId, keyword,
                provinceCode, districtCode, wardCode, pageable
        );

        LocalDateTime now = LocalDateTime.now();

        // ======================================================
        // 1️⃣ LỌC THEO GIÁ (APPLY CHO PRODUCT + VARIANT)
        // ======================================================
        List<Product> filtered = products.getContent().stream()
                .filter(p -> {

                    // --- Lấy giá thấp nhất ---
                    BigDecimal basePrice = (p.getFinalPrice() != null)
                            ? p.getFinalPrice()
                            : p.getPrice();

                    BigDecimal lowestPrice = basePrice;

                    if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                        BigDecimal minVariant = p.getVariants().stream()
                                .map(ProductVariantEntity::getVariantPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(basePrice);

                        lowestPrice = minVariant;
                    }

                    // --- Lọc theo minPrice ---
                    if (minPrice != null && lowestPrice.compareTo(minPrice) < 0)
                        return false;

                    // --- Lọc theo maxPrice ---
                    if (maxPrice != null && lowestPrice.compareTo(maxPrice) > 0)
                        return false;

                    return true;
                })

                // ======================================================
                // 2️⃣ LỌC THEO ĐÁNH GIÁ
                // ======================================================
                .filter(p ->
                        minRating == null
                                || (p.getRatingAverage() != null
                                && p.getRatingAverage().compareTo(minRating) >= 0)
                )
                .toList();

        // ======================================================
// 3️⃣ SORTING — THÊM NGAY Ở ĐÂY
// ======================================================
        Comparator<Product> comparator;

        switch (sortBy.toLowerCase()) {
            case "price" -> {
                comparator = Comparator.comparing(p -> {
                    BigDecimal basePrice = (p.getFinalPrice() != null)
                            ? p.getFinalPrice()
                            : p.getPrice();

                    if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                        return p.getVariants().stream()
                                .map(ProductVariantEntity::getVariantPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(basePrice);
                    }
                    return basePrice;
                });
            }
            default -> { // sortBy = name
                comparator = Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            }
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        filtered = filtered.stream()
                .sorted(comparator)
                .toList();

        // ======================================================
        // 3️⃣ XÂY DỰNG RESPONSE
        // ======================================================
        List<Map<String, Object>> data = filtered.stream().map(product -> {

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("productId", product.getProductId());
            p.put("name", product.getName());
            p.put("brandName", product.getBrandName());
            p.put("price", product.getPrice());
            p.put("discountPrice", product.getDiscountPrice());
            p.put("finalPrice", product.getFinalPrice());
            p.put("category", product.getCategory().getName());
            p.put("ratingAverage", product.getRatingAverage());
            p.put("reviewCount", product.getReviewCount());

            // Ảnh thumbnail
            p.put("thumbnailUrl",
                    product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0)
                            : null
            );

            // ========= VARIANTS =========
            p.put("variants", buildVariantList(product));

            // ========= STORE INFO =========
            Map<String, Object> storeMap = new LinkedHashMap<>();
            storeMap.put("id", product.getStore().getStoreId());
            storeMap.put("name", product.getStore().getStoreName());
            storeMap.put("status", product.getStore().getStatus());

            if (product.getStore().getStoreAddresses() != null &&
                    !product.getStore().getStoreAddresses().isEmpty()) {

                var addr = product.getStore().getStoreAddresses().get(0);
                storeMap.put("provinceCode", addr.getProvinceCode());
                storeMap.put("districtCode", addr.getDistrictCode());
                storeMap.put("wardCode", addr.getWardCode());
            } else {
                storeMap.put("provinceCode", null);
                storeMap.put("districtCode", null);
                storeMap.put("wardCode", null);
            }

            p.put("store", storeMap);

            // ========= VOUCHERS =========
            Map<String, Object> voucherMap = new LinkedHashMap<>();

            // → SHOP VOUCHERS
            shopVoucherProductRepo.findActiveShopVoucherProduct(product.getProductId(), now)
                    .ifPresent(svp -> {
                        var v = svp.getVoucher();
                        if (v != null && v.getStatus() == VoucherStatus.ACTIVE) {
                            Map<String, Object> shopVoucher = new LinkedHashMap<>();
                            shopVoucher.put("source", "SHOP");
                            shopVoucher.put("shopVoucherId", v.getId());
                            shopVoucher.put("shopVoucherProductId", svp.getId());
                            shopVoucher.put("code", v.getCode());
                            shopVoucher.put("title", v.getTitle());
                            shopVoucher.put("type", v.getType() != null ? v.getType().name() : null);
                            shopVoucher.put("discountValue", v.getDiscountValue());
                            shopVoucher.put("discountPercent", v.getDiscountPercent());
                            shopVoucher.put("maxDiscountValue", v.getMaxDiscountValue());
                            shopVoucher.put("minOrderValue", v.getMinOrderValue());
                            shopVoucher.put("startTime", v.getStartTime());
                            shopVoucher.put("endTime", v.getEndTime());
                            voucherMap.put("shopVoucher", shopVoucher);
                        }
                    });

            // → PLATFORM VOUCHERS
            List<PlatformCampaignProduct> activeMappings =
                    platformCampaignProductRepo.findAllActiveOnlyStatus(product.getProductId());

            if (!activeMappings.isEmpty()) {

                Map<UUID, List<PlatformCampaignProduct>> grouped =
                        activeMappings.stream().collect(
                                Collectors.groupingBy(cp -> cp.getCampaign().getId())
                        );

                List<Map<String, Object>> campaigns = new ArrayList<>();

                for (var entry : grouped.entrySet()) {
                    List<PlatformCampaignProduct> cps = entry.getValue();
                    PlatformCampaign c = cps.get(0).getCampaign();

                    Map<String, Object> cMap = new LinkedHashMap<>();
                    cMap.put("campaignId", c.getId());
                    cMap.put("code", c.getCode());
                    cMap.put("name", c.getName());
                    cMap.put("description", c.getDescription());
                    cMap.put("campaignType", c.getCampaignType());
                    cMap.put("badgeLabel", c.getBadgeLabel());
                    cMap.put("badgeColor", c.getBadgeColor());
                    cMap.put("badgeIconUrl", c.getBadgeIconUrl());
                    cMap.put("status", c.getStatus());
                    cMap.put("startTime", c.getStartTime());
                    cMap.put("endTime", c.getEndTime());

                    List<Map<String, Object>> voucherList = cps.stream().map(cp -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("platformVoucherId", cp.getId());
                        m.put("campaignId", c.getId());
                        m.put("type", cp.getType() != null ? cp.getType().name() : null);
                        m.put("discountValue", cp.getDiscountValue());
                        m.put("discountPercent", cp.getDiscountPercent());
                        m.put("maxDiscountValue", cp.getMaxDiscountValue());
                        m.put("minOrderValue", cp.getMinOrderValue());
                        m.put("totalVoucherIssued", cp.getTotalVoucherIssued());
                        m.put("totalUsageLimit", cp.getTotalUsageLimit());
                        m.put("usagePerUser", cp.getUsagePerUser());
                        m.put("status", cp.getStatus());
                        m.put("startTime", cp.getStartTime());
                        m.put("endTime", cp.getEndTime());

                        PlatformCampaignFlashSlot slot = cp.getFlashSlot();
                        if (c.getCampaignType() == CampaignType.FAST_SALE && slot != null) {
                            m.put("flashSlotId", slot.getId());
                            m.put("slotOpenTime", slot.getOpenTime());
                            m.put("slotCloseTime", slot.getCloseTime());
                            m.put("slotStatus", slot.getStatus());
                        }
                        return m;
                    }).toList();

                    cMap.put("vouchers", voucherList);
                    campaigns.add(cMap);
                }

                voucherMap.put("platformVouchers", campaigns);
            }

            if (!voucherMap.isEmpty()) p.put("vouchers", voucherMap);

            return p;
        }).toList();

        // ======================================================
        // 4️⃣ PAGINATION META
        // ======================================================
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("page", Map.of(
                "pageNumber", pageable.getPageNumber(),
                "pageSize", pageable.getPageSize(),
                "totalPages", totalPages,
                "totalElements", totalElements
        ));

        return ResponseEntity.ok(
                BaseResponse.success("✅ Lấy danh sách thumbnail thành công", result)
        );
    }

    // =========================================================
    // 2) PDP - GET ACTIVE VOUCHERS OF PRODUCT
    // =========================================================
    @Override
    public ResponseEntity<BaseResponse> getActiveVouchersOfProduct(UUID productId, String type, String campaignType) {

        LocalDateTime now = LocalDateTime.now();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 🔥 CHECK PRODUCT PHẢI ACTIVE — nếu không → báo lỗi luôn
        if (p.getStatus() != ProductStatus.ACTIVE) {
            return ResponseEntity
                    .badRequest()
                    .body(BaseResponse.error("❌ Product is not active"));
        }


        Map<String, Object> productMap = new LinkedHashMap<>();
        productMap.put("productId", p.getProductId());
        productMap.put("name", p.getName());
        productMap.put("price", p.getPrice());
        productMap.put("discountPrice", p.getDiscountPrice());
        productMap.put("finalPrice", p.getFinalPrice());
        productMap.put("brandName", p.getBrandName());
        productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
        productMap.put("thumbnailUrl", p.getImages() != null && !p.getImages().isEmpty()
                ? p.getImages().get(0)
                : null);

        // ADD VARIANTS
        productMap.put("variants", buildVariantList(p));

        Map<String, Object> vouchers = new LinkedHashMap<>();

        // SHOP VOUCHER
        if ("ALL".equalsIgnoreCase(type) || "SHOP".equalsIgnoreCase(type)) {
            shopVoucherProductRepo.findActiveShopVoucherProduct(productId, now)
                    .ifPresent(svp -> {
                        var v = svp.getVoucher();
                        if (v != null && v.getStatus() == VoucherStatus.ACTIVE) {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("source", "SHOP");
                            m.put("shopVoucherId", v.getId());
                            m.put("shopVoucherProductId", svp.getId());
                            m.put("code", v.getCode());
                            m.put("title", v.getTitle());
                            m.put("type", v.getType() != null ? v.getType().name() : null);
                            m.put("discountValue", v.getDiscountValue());
                            m.put("discountPercent", v.getDiscountPercent());
                            m.put("maxDiscountValue", v.getMaxDiscountValue());
                            m.put("minOrderValue", v.getMinOrderValue());
                            m.put("startTime", v.getStartTime());
                            m.put("endTime", v.getEndTime());
                            vouchers.put("shop", List.of(m));
                        }
                    });
        }

        // PLATFORM VOUCHER
        if ("ALL".equalsIgnoreCase(type) || "PLATFORM".equalsIgnoreCase(type)) {

            List<PlatformCampaignProduct> mappings =
                    platformCampaignProductRepo.findAllByProduct_ProductIdAndStatus(productId, VoucherStatus.ACTIVE);

            if (campaignType != null && !campaignType.isBlank()) {
                mappings = mappings.stream()
                        .filter(cp -> cp.getCampaign() != null &&
                                cp.getCampaign().getCampaignType().name().equalsIgnoreCase(campaignType))
                        .toList();
            }

            Map<UUID, List<PlatformCampaignProduct>> grouped =
                    mappings.stream().collect(Collectors.groupingBy(cp -> cp.getCampaign().getId()));

            List<Map<String, Object>> platform = new ArrayList<>();

            for (var entry : grouped.entrySet()) {
                List<PlatformCampaignProduct> cps = entry.getValue();
                PlatformCampaign c = cps.get(0).getCampaign();

                Map<String, Object> cMap = new LinkedHashMap<>();
                cMap.put("campaignId", c.getId());
                cMap.put("campaignType", c.getCampaignType());
                cMap.put("code", c.getCode());
                cMap.put("name", c.getName());
                cMap.put("description", c.getDescription());
                cMap.put("badgeLabel", c.getBadgeLabel());
                cMap.put("badgeColor", c.getBadgeColor());
                cMap.put("badgeIconUrl", c.getBadgeIconUrl());
                cMap.put("status", c.getStatus());
                cMap.put("startTime", c.getStartTime());
                cMap.put("endTime", c.getEndTime());

                List<Map<String, Object>> voucherList = cps.stream().map(cp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("platformVoucherId", cp.getId());
                    m.put("campaignId", c.getId());
                    m.put("type", cp.getType() != null ? cp.getType().name() : null);
                    m.put("discountValue", cp.getDiscountValue());
                    m.put("discountPercent", cp.getDiscountPercent());
                    m.put("maxDiscountValue", cp.getMaxDiscountValue());
                    m.put("minOrderValue", cp.getMinOrderValue());
                    m.put("totalVoucherIssued", cp.getTotalVoucherIssued());
                    m.put("totalUsageLimit", cp.getTotalUsageLimit());
                    m.put("usagePerUser", cp.getUsagePerUser());
                    m.put("status", cp.getStatus());
                    m.put("startTime", cp.getStartTime());
                    m.put("endTime", cp.getEndTime());

                    PlatformCampaignFlashSlot slot = cp.getFlashSlot();
                    if (c.getCampaignType() == CampaignType.FAST_SALE && slot != null) {
                        m.put("flashSlotId", slot.getId());
                        m.put("slotOpenTime", slot.getOpenTime());
                        m.put("slotCloseTime", slot.getCloseTime());
                        m.put("slotStatus", slot.getStatus());
                    }

                    return m;
                }).toList();

                cMap.put("vouchers", voucherList);
                platform.add(cMap);
            }

            if (!platform.isEmpty()) vouchers.put("platform", platform);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", productMap);
        result.put("vouchers", vouchers);

        return ResponseEntity.ok(BaseResponse.success("✅ Active vouchers fetched (FULL)", result));
    }

    // =========================================================
    // BUILD VARIANTS
    // =========================================================
    private List<Map<String, Object>> buildVariantList(Product product) {
        if (product.getVariants() == null) return List.of();

        return product.getVariants().stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("variantId", v.getId());
            m.put("optionName", v.getOptionName());     // Color / Size / Version
            m.put("optionValue", v.getOptionValue());   // Black / M / 256GB
            m.put("variantSku", v.getVariantSku());
            m.put("price", v.getVariantPrice());
            m.put("stock", v.getVariantStock());
            m.put("imageUrl", v.getVariantUrl());
            return m;
        }).toList();
    }

}
