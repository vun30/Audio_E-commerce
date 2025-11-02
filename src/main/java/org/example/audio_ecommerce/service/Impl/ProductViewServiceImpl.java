package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.PlatformCampaign;
import org.example.audio_ecommerce.entity.Product;
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

        // 🧩 Lọc sản phẩm
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
                    ? product.getImages().get(0)
                    : null);
            p.put("ratingAverage", product.getRatingAverage());
            p.put("reviewCount", product.getReviewCount());

            // 🏪 STORE INFO
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

            // 🎫 VOUCHERS
            Map<String, Object> voucherMap = new LinkedHashMap<>();

            // 🔹 Shop Voucher (chỉ lấy nếu ACTIVE)
            shopVoucherProductRepo.findActiveVoucherByProduct(product.getProductId(), now)
                    .filter(v -> v.getStatus() == VoucherStatus.ACTIVE)
                    .ifPresent(v -> {
                        Map<String, Object> shopVoucher = new LinkedHashMap<>();
                        shopVoucher.put("source", "SHOP");
                        shopVoucher.put("code", v.getCode());
                        shopVoucher.put("title", v.getTitle());
                        shopVoucher.put("type", v.getType().name());
                        shopVoucher.put("discountValue", v.getDiscountValue());
                        shopVoucher.put("discountPercent", v.getDiscountPercent());
                        shopVoucher.put("maxDiscountValue", v.getMaxDiscountValue());
                        shopVoucher.put("startTime", v.getStartTime());
                        shopVoucher.put("endTime", v.getEndTime());
                        voucherMap.put("shopVoucher", shopVoucher);
                    });

            // 🔹 Platform Voucher (chỉ lấy nếu ACTIVE)
            platformCampaignProductRepo.findActiveCampaignVoucherByProduct(product.getProductId(), now)
                    .filter(cp -> cp.getStatus() == VoucherStatus.ACTIVE && cp.getCampaign() != null)
                    .ifPresent(cp -> {
                        PlatformCampaign c = cp.getCampaign();
                        if (c.getStatus() == VoucherStatus.ACTIVE) {
                            Map<String, Object> campaignMap = new LinkedHashMap<>();
                            campaignMap.put("id", c.getId());
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

                            // 🕓 Nếu là FAST_SALE → có Flash Slots
                            if (c.getFlashSlots() != null && !c.getFlashSlots().isEmpty()) {
                                List<Map<String, Object>> slots = c.getFlashSlots().stream()
                                        .sorted(Comparator.comparing(slot -> slot.getOpenTime()))
                                        .map(slot -> {
                                            Map<String, Object> m = new LinkedHashMap<>();
                                            m.put("id", slot.getId());
                                            m.put("openTime", slot.getOpenTime());
                                            m.put("closeTime", slot.getCloseTime());
                                            m.put("status", slot.getStatus());

                                            // ✅ Thêm thông tin discount riêng từng slot
                                            m.put("type", cp.getType().name());
                                            m.put("discountValue", cp.getDiscountValue());
                                            m.put("discountPercent", cp.getDiscountPercent());
                                            m.put("maxDiscountValue", cp.getMaxDiscountValue());
                                            m.put("minOrderValue", cp.getMinOrderValue());
                                            m.put("totalVoucherIssued", cp.getTotalVoucherIssued());
                                            m.put("totalUsageLimit", cp.getTotalUsageLimit());
                                            m.put("usagePerUser", cp.getUsagePerUser());

                                            return m;
                                        })
                                        .collect(Collectors.toList());
                                campaignMap.put("flashSlots", slots);
                            }
                            // 🟢 Nếu là MEGA_SALE → không có slot, hiển thị discount trực tiếp
                            else {
                                campaignMap.put("type", cp.getType().name());
                                campaignMap.put("discountValue", cp.getDiscountValue());
                                campaignMap.put("discountPercent", cp.getDiscountPercent());
                                campaignMap.put("maxDiscountValue", cp.getMaxDiscountValue());
                                campaignMap.put("minOrderValue", cp.getMinOrderValue());
                                campaignMap.put("totalVoucherIssued", cp.getTotalVoucherIssued());
                                campaignMap.put("totalUsageLimit", cp.getTotalUsageLimit());
                                campaignMap.put("usagePerUser", cp.getUsagePerUser());
                            }

                            voucherMap.put("platformVoucher", campaignMap);
                        }
                    });

            // ✅ Chỉ thêm nếu có ít nhất 1 voucher
            if (!voucherMap.isEmpty()) {
                p.put("vouchers", voucherMap);
            }

            return p;
        }).toList();

        // 📦 Pagination
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

    @Override
public ResponseEntity<BaseResponse> getActiveVouchersOfProduct(UUID productId, String type, String campaignType) {

    LocalDateTime now = LocalDateTime.now();

    // product info
    Product p = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    Map<String,Object> productMap = new LinkedHashMap<>();
    productMap.put("productId", p.getProductId());
    productMap.put("name", p.getName());
    productMap.put("price", p.getPrice());
    productMap.put("discountPrice", p.getDiscountPrice());
    productMap.put("finalPrice", p.getFinalPrice());


    Map<String,Object> vouchers = new LinkedHashMap<>();

    // =============== SHOP voucher ==================
    if(type.equals("ALL") || type.equals("SHOP")){
        shopVoucherProductRepo.findActiveVoucherByProduct(productId, now).ifPresent(v -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("source","SHOP");
            m.put("code", v.getCode());
            m.put("title", v.getTitle());
            m.put("type", v.getType());
            m.put("discountValue", v.getDiscountValue());
            m.put("discountPercent", v.getDiscountPercent());
            m.put("maxDiscountValue", v.getMaxDiscountValue());
            vouchers.put("shop", List.of(m));
        });
    }


    // =============== PLATFORM voucher ==================
    if(type.equals("ALL") || type.equals("PLATFORM")){
        platformCampaignProductRepo.findActiveCampaignVoucherByProduct(productId, now).ifPresent(cp -> {

            var c = cp.getCampaign();

            // filter theo campaignType nếu FE truyền
            if(campaignType!=null && !c.getCampaignType().name().equals(campaignType)) return;

            Map<String,Object> map = new LinkedHashMap<>();
            map.put("campaignId", c.getId());
            map.put("campaignType", c.getCampaignType());
            map.put("code", c.getCode());
            map.put("discountValue", cp.getDiscountValue());
            map.put("discountPercent", cp.getDiscountPercent());
            map.put("maxDiscountValue", cp.getMaxDiscountValue());

            // nếu FAST SALE thì trả slot
            if(c.getFlashSlots()!=null && !c.getFlashSlots().isEmpty()){
                List<Map<String,Object>> slots = c.getFlashSlots().stream()
                        .sorted(Comparator.comparing(s->s.getOpenTime()))
                        .map(s->{
                            Map<String,Object> sm = new LinkedHashMap<>();
                            sm.put("slotId", s.getId());
                            sm.put("openTime", s.getOpenTime());
                            sm.put("closeTime", s.getCloseTime());
                            sm.put("discountValue", cp.getDiscountValue());
                            sm.put("discountPercent", cp.getDiscountPercent());
                            return sm;
                        }).toList();
                map.put("slots", slots);
            }

            vouchers.put("platform", List.of(map));
        });
    }


    Map<String,Object> result = new LinkedHashMap<>();
    result.put("product", productMap);
    result.put("vouchers", vouchers);

    return ResponseEntity.ok(BaseResponse.success("✅ Active vouchers fetched", result));
}

}
