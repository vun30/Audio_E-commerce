package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherRepository;
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
    private final ShopVoucherRepository shopVoucherRepository;

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
        Integer minReviewCount,
        Integer minViewCount,
        Integer minSellCount,
        Pageable pageable,
        String sortBy,
        String sortDir
) {

    // ==========================================
    // ✔ Convert status -> ENUM chính xác
    // ==========================================
    ProductStatus statusEnum = null;
    if (status != null && !status.isBlank()) {
        try {
            statusEnum = ProductStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("❌ Invalid product status: " + status));
        }
    }

    // ==========================================
    // ✔ GỌI REPOSITORY ĐÚNG — ĐIỂM BỊ THIẾU
    // ==========================================
    Page<Product> products = productRepo.findAllWithAdvancedFilters(
            statusEnum,
            categoryId,
            storeId,
            keyword,
            provinceCode,
            districtCode,
            wardCode,
            pageable
    );

    LocalDateTime now = LocalDateTime.now();

    // ======================================================
    // 🔥 FILTER PRICE (PRODUCT + VARIANT)
    // ======================================================
    List<Product> filtered = products.getContent().stream()
            .filter(p -> {
                BigDecimal basePrice = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();

                BigDecimal lowestPrice = basePrice;
                if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    lowestPrice = p.getVariants().stream()
                            .map(ProductVariantEntity::getVariantPrice)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(basePrice);
                }

                if (minPrice != null && lowestPrice.compareTo(minPrice) < 0) return false;
                if (maxPrice != null && lowestPrice.compareTo(maxPrice) > 0) return false;

                return true;
            })

            // ======================================================
            // 🔥 FILTER RATING
            // ======================================================
            .filter(p ->
                    minRating == null
                            || (p.getRatingAverage() != null
                            && p.getRatingAverage().compareTo(minRating) >= 0)
            )

            // ======================================================
            // 🔥 FILTER COUNTS
            // ======================================================
            .filter(p ->
                    (minReviewCount == null || (p.getReviewCount() != null && p.getReviewCount() >= minReviewCount))
                            && (minViewCount == null || (p.getViewCount() != null && p.getViewCount() >= minViewCount))
                            && (minSellCount == null || (p.getSellCount() != null && p.getSellCount() >= minSellCount))
            )

            // ======================================================
            // 🔥 FUZZY SEARCH (Name + Brand + Desc)
            // ======================================================
            .filter(p -> {
                if (keyword == null || keyword.isBlank()) return true;
                return fuzzyMatch(p.getName(), keyword)
                        || fuzzyMatch(p.getBrandName(), keyword)
                        || fuzzyMatch(p.getDescription(), keyword);
            })

            .toList();

    // ======================================================
    // 🔥 SORTING
    // ======================================================
    Comparator<Product> comparator;

    String safeSortBy = sortBy != null ? sortBy.toLowerCase() : "name";

    switch (safeSortBy) {
        case "price" -> {
            comparator = Comparator.comparing(p -> {
                BigDecimal basePrice = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();
                if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    return p.getVariants().stream()
                            .map(ProductVariantEntity::getVariantPrice)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(basePrice);
                }
                return basePrice;
            }, Comparator.nullsLast(BigDecimal::compareTo));
        }
        case "view", "viewcount" ->
                comparator = Comparator.comparing(p -> Optional.ofNullable(p.getViewCount()).orElse(0));
        case "review", "reviewcount" ->
                comparator = Comparator.comparing(p -> Optional.ofNullable(p.getReviewCount()).orElse(0));
        case "rating", "ratingaverage" ->
                comparator = Comparator.comparing(p -> Optional.ofNullable(p.getRatingAverage()).orElse(BigDecimal.ZERO));
        case "sell", "sellcount" ->
                comparator = Comparator.comparing(p -> Optional.ofNullable(p.getSellCount()).orElse(0));
        default -> comparator = Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
    }

    if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();

    filtered = filtered.stream().sorted(comparator).toList();

    // ======================================================
    // 🔥 BUILD RESPONSE
    // ======================================================
    List<Map<String, Object>> data = filtered.stream().map(product -> {

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("productId", product.getProductId());
        p.put("name", product.getName());
        p.put("brandName", product.getBrandName());
        p.put("price", product.getPrice());
        p.put("discountPrice", product.getDiscountPrice());
        p.put("finalPrice", product.getFinalPrice());
        p.put("ratingAverage", product.getRatingAverage());
        p.put("reviewCount", product.getReviewCount());

        // ⭐ MULTI CATEGORY FIX
        p.put("categories",
                product.getCategories() == null ? List.of() :
                        product.getCategories().stream()
                                .map(c -> Map.of(
                                        "categoryId", c.getCategoryId(),
                                        "categoryName", c.getName()
                                ))
                                .toList()
        );

        p.put("thumbnailUrl",
                (product.getImages() != null && !product.getImages().isEmpty())
                        ? product.getImages().get(0)
                        : null
        );

        p.put("variants", buildVariantList(product));

        // STORE
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

        // VOUCHERS
        Map<String, Object> voucherMap = new LinkedHashMap<>();

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
                        shopVoucher.put("discountValue", v.getDiscountValue());
                        shopVoucher.put("discountPercent", v.getDiscountPercent());
                        shopVoucher.put("maxDiscountValue", v.getMaxDiscountValue());
                        shopVoucher.put("minOrderValue", v.getMinOrderValue());
                        shopVoucher.put("startTime", v.getStartTime());
                        shopVoucher.put("endTime", v.getEndTime());

                        voucherMap.put("shopVoucher", shopVoucher);
                    }
                });

        // PLATFORM
        List<PlatformCampaignProduct> activeMappings =
                platformCampaignProductRepo.findAllActiveOnlyStatus(product.getProductId());

        if (!activeMappings.isEmpty()) {
            Map<UUID, List<PlatformCampaignProduct>> grouped =
                    activeMappings.stream()
                            .collect(Collectors.groupingBy(cp -> cp.getCampaign().getId()));

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

                List<Map<String, Object>> voucherList = cps.stream().map(cp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("platformVoucherId", cp.getId());
                    m.put("campaignId", c.getId());
                    m.put("type", cp.getType() != null ? cp.getType().name() : null);
                    m.put("discountValue", cp.getDiscountValue());
                    m.put("discountPercent", cp.getDiscountPercent());
                    m.put("maxDiscountValue", cp.getMaxDiscountValue());
                    m.put("minOrderValue", cp.getMinOrderValue());
                    m.put("usagePerUser", cp.getUsagePerUser());
                    m.put("status", cp.getStatus());
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
    // PAGINATION
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

    return ResponseEntity.ok(BaseResponse.success("✅ Lấy danh sách thumbnail thành công", result));
}


    // =========================================================
    // 2) PDP – ACTIVE VOUCHERS
    @Override
    public ResponseEntity<BaseResponse> getActiveVouchersOfProduct(UUID productId, String type, String campaignType) {

        final LocalDateTime now = LocalDateTime.now(); // nếu bạn đang lưu time bị -7h thì cân nhắc now.minusHours(7)

        // ✅ type là BẮT BUỘC
        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("❌ type is required (ALL | SHOP | PLATFORM)"));
        }
        final String safeType = type.trim().toUpperCase();

        // ✅ validate type
        if (!Set.of("ALL", "SHOP", "PLATFORM").contains(safeType)) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("❌ Invalid type: " + type + " (must be ALL | SHOP | PLATFORM)"));
        }

        // ✅ campaignType OPTIONAL
        final CampaignType campaignTypeEnum;
        if (campaignType != null && !campaignType.isBlank()) {
            try {
                campaignTypeEnum = CampaignType.valueOf(campaignType.trim().toUpperCase());
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("❌ Invalid campaignType: " + campaignType));
            }
        } else {
            campaignTypeEnum = null;
        }

        // ✅ Find product
        final Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // ✅ Only ACTIVE product
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("❌ Product is not active"));
        }

        // ======================================================
        // BUILD PRODUCT JSON
        // ======================================================
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("productId", product.getProductId());
        p.put("name", product.getName());
        p.put("brandName", product.getBrandName());
        p.put("price", product.getPrice());
        p.put("discountPrice", product.getDiscountPrice());
        p.put("finalPrice", product.getFinalPrice());
        p.put("ratingAverage", product.getRatingAverage());
        p.put("reviewCount", product.getReviewCount());

        p.put("categories",
                product.getCategories() == null ? List.of() :
                        product.getCategories().stream()
                                .map(c -> Map.of(
                                        "categoryId", c.getCategoryId(),
                                        "categoryName", c.getName()
                                ))
                                .toList()
        );

        p.put("thumbnailUrl",
                (product.getImages() != null && !product.getImages().isEmpty())
                        ? product.getImages().get(0)
                        : null
        );

        p.put("variants", buildVariantList(product));

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

        // ======================================================
        // BUILD VOUCHERS JSON
        // ======================================================
        Map<String, Object> voucherMap = new LinkedHashMap<>();

        // ---------- SHOP (GET CẢ 2 LOẠI: PRODUCT + ALL_SHOP) ----------
        if ("ALL".equals(safeType) || "SHOP".equals(safeType)) {

            UUID storeId = product.getStore().getStoreId();

            List<Map<String, Object>> shopVouchers = new ArrayList<>();

            // (1) Voucher gắn theo product (qua ShopVoucherProduct)
            shopVoucherProductRepo.findActiveShopVoucherProduct(product.getProductId(), now)
                    .ifPresent(svp -> {
                        var v = svp.getVoucher();
                        if (v != null && v.getStatus() == VoucherStatus.ACTIVE) {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("source", "SHOP");
                            m.put("scopeType", v.getScopeType()); // PRODUCT_VOUCHER
                            m.put("shopVoucherId", v.getId());
                            m.put("shopVoucherProductId", svp.getId());
                            m.put("code", v.getCode());
                            m.put("title", v.getTitle());
                            m.put("discountValue", v.getDiscountValue());
                            m.put("discountPercent", v.getDiscountPercent());
                            m.put("maxDiscountValue", v.getMaxDiscountValue());
                            m.put("minOrderValue", v.getMinOrderValue());
                            m.put("startTime", v.getStartTime());
                            m.put("endTime", v.getEndTime());
                            shopVouchers.add(m);
                        }
                    });

            // (2) Voucher toàn shop (không cần mapping product)
            // ⚠️ shopVoucherRepo là ShopVoucherRepository (đặt đúng tên bean của bạn)
            List<ShopVoucher> activeShopWide = shopVoucherRepository.findAllActiveByStore(storeId, now);

            for (ShopVoucher v : activeShopWide) {
                if (v.getScopeType() != ShopVoucherScopeType.ALL_SHOP_VOUCHER) continue; // chỉ lấy voucher toàn shop

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("source", "SHOP");
                m.put("scopeType", v.getScopeType()); // ALL_SHOP_VOUCHER
                m.put("shopVoucherId", v.getId());
                m.put("code", v.getCode());
                m.put("title", v.getTitle());
                m.put("discountValue", v.getDiscountValue());
                m.put("discountPercent", v.getDiscountPercent());
                m.put("maxDiscountValue", v.getMaxDiscountValue());
                m.put("minOrderValue", v.getMinOrderValue());
                m.put("startTime", v.getStartTime());
                m.put("endTime", v.getEndTime());
                shopVouchers.add(m);
            }

            if (!shopVouchers.isEmpty()) {
                voucherMap.put("shopVouchers", shopVouchers);
            }
        }

        // ---------- PLATFORM (GIỐNG GET ALL: chỉ lấy status ACTIVE) ----------
        if ("ALL".equals(safeType) || "PLATFORM".equals(safeType)) {

            final List<PlatformCampaignProduct> activeMappings =
                    platformCampaignProductRepo.findAllActiveOnlyStatus(product.getProductId());

            final List<PlatformCampaignProduct> filteredMappings =
                    (campaignTypeEnum == null)
                            ? activeMappings
                            : activeMappings.stream()
                            .filter(cp -> cp.getCampaign() != null
                                    && cp.getCampaign().getCampaignType() == campaignTypeEnum)
                            .toList();

            if (!filteredMappings.isEmpty()) {
                Map<UUID, List<PlatformCampaignProduct>> grouped =
                        filteredMappings.stream()
                                .filter(cp -> cp.getCampaign() != null)
                                .collect(Collectors.groupingBy(cp -> cp.getCampaign().getId()));

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

                    List<Map<String, Object>> voucherList = cps.stream().map(cp -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("platformVoucherId", cp.getId());
                        m.put("campaignId", c.getId());
                        m.put("type", cp.getType() != null ? cp.getType().name() : null);
                        m.put("discountValue", cp.getDiscountValue());
                        m.put("discountPercent", cp.getDiscountPercent());
                        m.put("maxDiscountValue", cp.getMaxDiscountValue());
                        m.put("minOrderValue", cp.getMinOrderValue());
                        m.put("usagePerUser", cp.getUsagePerUser());
                        m.put("status", cp.getStatus());
                        return m;
                    }).toList();

                    cMap.put("vouchers", voucherList);
                    campaigns.add(cMap);
                }

                voucherMap.put("platformVouchers", campaigns);
            }
        }

        p.put("vouchers", voucherMap);

        return ResponseEntity.ok(BaseResponse.success("✅ Product fetched with active vouchers", p));
    }

    // =========================================================
    // BUILD VARIANTS
    // =========================================================
    private List<Map<String, Object>> buildVariantList(Product product) {
        if (product.getVariants() == null) return List.of();
        return product.getVariants().stream().map(v -> Map.<String, Object>of(
                "variantId", v.getId(),
                "optionName", v.getOptionName(),
                "optionValue", v.getOptionValue(),
                "variantSku", v.getVariantSku(),
                "price", v.getVariantPrice(),
                "stock", v.getVariantStock(),
                "imageUrl", v.getVariantUrl()
        )).toList();
    }

    // =========================================================
    // FUZZY SEARCH
    // =========================================================
    private boolean fuzzyMatch(String text, String keyword) {
        if (text == null || keyword == null) return false;
        return text.toLowerCase().contains(keyword.toLowerCase().trim());
    }
}
