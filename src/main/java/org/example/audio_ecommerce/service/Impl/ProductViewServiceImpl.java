package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.CampaignType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.PlatformCampaign;
import org.example.audio_ecommerce.entity.PlatformCampaignFlashSlot;
import org.example.audio_ecommerce.entity.PlatformCampaignProduct;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.ShopVoucherProduct;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherProductRepository;
import org.example.audio_ecommerce.service.ProductViewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductViewServiceImpl implements ProductViewService {

    private final ProductRepository productRepo;
    private final ShopVoucherProductRepository shopVoucherProductRepo;
    private final PlatformCampaignProductRepository platformCampaignProductRepo;

    // ==============================
    // 1) Thumbnail list (giữ nguyên, nhưng bổ sung id mapping cho shop/platform)
    // ==============================
    @Override
    public ResponseEntity<BaseResponse> getThumbnailView(
            String status,
            UUID categoryId,
            UUID storeId,
            String keyword,
            String provinceCode,
            String districtCode,
            String wardCode,
            Pageable pageable) {

        Page<Product> products = productRepo.findAllWithAdvancedFilters(
                status, categoryId, storeId, keyword,
                provinceCode, districtCode, wardCode, pageable
        );

        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> data = products.stream().map(product -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("productId", product.getProductId());
            p.put("name", product.getName());
            p.put("brandName", product.getBrandName());
            p.put("price", product.getPrice());
            p.put("discountPrice", product.getDiscountPrice());
            p.put("finalPrice", product.getFinalPrice());
            p.put("category", product.getCategory().getName());
            p.put("thumbnailUrl", (product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0) : null);
            p.put("ratingAverage", product.getRatingAverage());
            p.put("reviewCount", product.getReviewCount());

            Map<String, Object> storeMap = new LinkedHashMap<>();
            storeMap.put("id", product.getStore().getStoreId());
            storeMap.put("name", product.getStore().getStoreName());
            storeMap.put("status", product.getStore().getStatus());
            if (!product.getStore().getStoreAddresses().isEmpty()) {
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

            Map<String, Object> voucherMap = new LinkedHashMap<>();

            // ===== Shop voucher (ACTIVE) kèm 2 id =====
            shopVoucherProductRepo.findActiveShopVoucherProduct(product.getProductId(), now)
                    .ifPresent(svp -> {
                        var v = svp.getVoucher();
                        if (v != null && v.getStatus() == VoucherStatus.ACTIVE) {
                            Map<String, Object> shopVoucher = new LinkedHashMap<>();
                            shopVoucher.put("source", "SHOP");
                            shopVoucher.put("shopVoucherId", v.getId());            // id voucher chính
                            shopVoucher.put("shopVoucherProductId", svp.getId());   // id mapping
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

            // ===== Platform voucher (ACTIVE) – trả FULL (A): tất cả mapping/slot đang hợp lệ theo now() =====
            List<PlatformCampaignProduct> activeMappings =
                    platformCampaignProductRepo.findAllActiveByProduct(product.getProductId(), now);

            if (!activeMappings.isEmpty()) {
                // Nhóm theo campaign để FE dễ hiển thị (nếu muốn badge theo chiến dịch)
                Map<UUID, List<PlatformCampaignProduct>> byCampaign =
                        activeMappings.stream().collect(Collectors.groupingBy(cp -> cp.getCampaign().getId()));

                List<Map<String, Object>> campaigns = new ArrayList<>();
                for (var entry : byCampaign.entrySet()) {
                    List<PlatformCampaignProduct> cps = entry.getValue();
                    PlatformCampaign c = cps.get(0).getCampaign();

                    Map<String, Object> campaignMap = new LinkedHashMap<>();
                    campaignMap.put("campaignId", c.getId());
                    campaignMap.put("code", c.getCode());
                    campaignMap.put("name", c.getName());
                    campaignMap.put("description", c.getDescription());
                    campaignMap.put("campaignType", c.getCampaignType());
                    campaignMap.put("badgeLabel", c.getBadgeLabel());
                    campaignMap.put("badgeColor", c.getBadgeColor());
                    campaignMap.put("badgeIconUrl", c.getBadgeIconUrl());
                    campaignMap.put("status", c.getStatus());
                    campaignMap.put("allowRegistration", c.getAllowRegistration());
                    campaignMap.put("approvalRule", c.getApprovalRule());
                    campaignMap.put("startTime", c.getStartTime());
                    campaignMap.put("endTime", c.getEndTime());
                    campaignMap.put("createdAt", c.getCreatedAt());
                    campaignMap.put("createdBy", c.getCreatedBy());

                    // Convert từng mapping (mỗi mapping tương ứng 1 voucher áp vào product; FAST thì mỗi mapping gắn 1 slot)
                    List<Map<String, Object>> vouchers = cps.stream().map(cp -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("platformVoucherId", cp.getId());         // id mapping PlatformCampaignProduct
                        m.put("campaignId", c.getId());                 // id campaign
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

                        // Nếu FAST_SALE thì mapping có slot cụ thể → append flashSlotId
                        PlatformCampaignFlashSlot slot = cp.getFlashSlot();
                        if (c.getCampaignType() == CampaignType.FAST_SALE && slot != null) {
                            m.put("flashSlotId", slot.getId());
                            m.put("slotOpenTime", slot.getOpenTime());
                            m.put("slotCloseTime", slot.getCloseTime());
                            m.put("slotStatus", slot.getStatus());
                        }
                        return m;
                    }).toList();

                    // Với MEGA_SALE: Không có slot, vouchers list vẫn hợp lệ (mỗi mapping = 1 cấu hình giảm giá)
                    campaignMap.put("vouchers", vouchers);
                    campaigns.add(campaignMap);
                }
                voucherMap.put("platformVouchers", campaigns);
            }

            if (!voucherMap.isEmpty()) {
                p.put("vouchers", voucherMap);
            }

            return p;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("page", Map.of(
                "pageNumber", products.getNumber(),
                "pageSize", products.getSize(),
                "totalPages", products.getTotalPages(),
                "totalElements", products.getTotalElements()
        ));

        return ResponseEntity.ok(BaseResponse.success("✅ Lấy danh sách thumbnail thành công", result));
    }

    // ==============================
    // 2) PDP: trả FULL product + toàn bộ voucher ACTIVE của product (A)
    // ==============================
    @Override
    public ResponseEntity<BaseResponse> getActiveVouchersOfProduct(UUID productId, String type, String campaignType) {
        LocalDateTime now = LocalDateTime.now();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Map<String, Object> productMap = new LinkedHashMap<>();
        productMap.put("productId", p.getProductId());
        productMap.put("name", p.getName());
        productMap.put("price", p.getPrice());
        productMap.put("discountPrice", p.getDiscountPrice());
        productMap.put("finalPrice", p.getFinalPrice());
        productMap.put("brandName", p.getBrandName());
        productMap.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
        productMap.put("thumbnailUrl", (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0) : null);

        Map<String, Object> vouchers = new LinkedHashMap<>();

        // === SHOP voucher (ACTIVE) + 2 id ===
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

        // === PLATFORM vouchers (ACTIVE), FULL (A) ===
        if ("ALL".equalsIgnoreCase(type) || "PLATFORM".equalsIgnoreCase(type)) {
            List<PlatformCampaignProduct> mappings = platformCampaignProductRepo.findAllActiveByProduct(productId, now);

            if (campaignType != null && !campaignType.isBlank()) {
                mappings = mappings.stream()
                        .filter(cp -> cp.getCampaign() != null
                                && cp.getCampaign().getCampaignType().name().equalsIgnoreCase(campaignType))
                        .toList();
            }

            // Nhóm theo Campaign
            Map<UUID, List<PlatformCampaignProduct>> byCampaign =
                    mappings.stream().collect(Collectors.groupingBy(cp -> cp.getCampaign().getId()));

            List<Map<String, Object>> platform = new ArrayList<>();
            for (var entry : byCampaign.entrySet()) {
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

                // Mỗi mapping = 1 voucher áp cho product; FAST có slot riêng → flashSlotId
                List<Map<String, Object>> voucherList = cps.stream().map(cp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("platformVoucherId", cp.getId());       // mapping id (PlatformCampaignProduct.id)
                    m.put("campaignId", c.getId());               // campaign id
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

            if (!platform.isEmpty()) {
                vouchers.put("platform", platform);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", productMap);
        result.put("vouchers", vouchers);

        return ResponseEntity.ok(BaseResponse.success("✅ Active vouchers fetched (FULL)", result));
    }
}
