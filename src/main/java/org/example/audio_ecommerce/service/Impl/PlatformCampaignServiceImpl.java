// org.example.audio_ecommerce.service.Impl.PlatformCampaignServiceImpl
package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.PlatformCampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformCampaignServiceImpl implements PlatformCampaignService {

    private final PlatformCampaignRepository campaignRepository;
    private final PlatformCampaignFlashSlotRepository flashSlotRepository;
    private final PlatformCampaignProductRepository campaignProductRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    // =====================================================
    // 1) TẠO CAMPAIGN HỢP NHẤT
    // =====================================================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> createCampaignUnified(CreateOrUpdateCampaignRequest req) {
        if (campaignRepository.existsByCodeIgnoreCase(req.getCode()))
            throw new RuntimeException("❌ Campaign code already exists");

        if (req.getCampaignType() == null)
            throw new RuntimeException("❌ campaignType is required (MEGA_SALE / FAST_SALE)");

        PlatformCampaign campaign = PlatformCampaign.builder()
                .code(req.getCode())
                .name(req.getName())
                .description(req.getDescription())
                .campaignType(req.getCampaignType())
                .badgeLabel(Optional.ofNullable(req.getBadgeLabel())
                        .orElse(req.getCampaignType() == CampaignType.FAST_SALE ? "Flash Sale" : "Mega Sale"))
                .badgeColor(Optional.ofNullable(req.getBadgeColor())
                        .orElse(req.getCampaignType() == CampaignType.FAST_SALE ? "#FF6600" : "#00AA88"))
                .badgeIconUrl(Optional.ofNullable(req.getBadgeIconUrl())
                        .orElse(req.getCampaignType() == CampaignType.FAST_SALE
                                ? "https://cdn.audiohub.vn/badges/flashsale.png"
                                : "https://cdn.audiohub.vn/badges/megasale.png"))
                .allowRegistration(Optional.ofNullable(req.getAllowRegistration()).orElse(true))
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(VoucherStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        campaignRepository.save(campaign);

        // Nếu FAST_SALE → tạo slots
        if (req.getCampaignType() == CampaignType.FAST_SALE) {
            if (req.getFlashSlots() == null || req.getFlashSlots().isEmpty()) {
                throw new RuntimeException("❌ FAST_SALE requires flashSlots in request");
            }
            for (var s : req.getFlashSlots()) {
                if (s.getOpenTime() == null || s.getCloseTime() == null || !s.getCloseTime().isAfter(s.getOpenTime())) {
                    throw new RuntimeException("❌ Invalid slot time");
                }
                flashSlotRepository.save(PlatformCampaignFlashSlot.builder()
                        .campaign(campaign)
                        .openTime(s.getOpenTime())
                        .closeTime(s.getCloseTime())
                        .status(SlotStatus.PENDING)
                        .build());
            }
        }

        var res = CampaignWithSlotsResponse.builder()
                .id(campaign.getId())
                .code(campaign.getCode())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .campaignType(campaign.getCampaignType())
                .badgeLabel(campaign.getBadgeLabel())
                .badgeColor(campaign.getBadgeColor())
                .badgeIconUrl(campaign.getBadgeIconUrl())
                .allowRegistration(campaign.getAllowRegistration())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus())
                .slots(req.getCampaignType() == CampaignType.FAST_SALE
                        ? flashSlotRepository.findAllByCampaign_Id(campaign.getId()).stream()
                            .map(sl -> CampaignWithSlotsResponse.SlotDto.builder()
                                    .id(sl.getId())
                                    .openTime(sl.getOpenTime())
                                    .closeTime(sl.getCloseTime())
                                    .status(sl.getStatus())
                                    .build())
                            .toList()
                        : null)
                .build();

        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Campaign created", res));
    }

    // =====================================================
    // 2) STORE THAM GIA CAMPAIGN (THÊM SẢN PHẨM)
    // =====================================================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> joinCampaign(UUID campaignId, CampaignProductRegisterRequest req) {
        Store store = getCurrentStore();

        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign not found"));

        if (!Boolean.TRUE.equals(campaign.getAllowRegistration()))
            throw new RuntimeException("🚫 Registration disabled for this campaign");

        // Điều kiện store: ACTIVE + có >=1 product ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(store.getStatus().name()))
            throw new RuntimeException("🚫 Store must be ACTIVE");

        long activeProducts = productRepository.countByStore_StoreIdAndStatus(store.getStoreId(), ProductStatus.ACTIVE);
        if (activeProducts < 1)
            throw new RuntimeException("🚫 Store must have at least 1 ACTIVE product");

        List<PlatformCampaignProduct> toSave = new ArrayList<>();

        boolean isFastSale = campaign.getCampaignType() == CampaignType.FAST_SALE;

        for (var item : req.getProducts()) {
            var product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("❌ Product not found: " + item.getProductId()));

            if (!product.getStore().getStoreId().equals(store.getStoreId()))
                throw new RuntimeException("🚫 Product does not belong to current store: " + product.getName());

            // ✅ Rule: product đã cập nhật cách đây >= 7 ngày (dựa vào lastUpdateIntervalDays)
            if (product.getLastUpdateIntervalDays() != null && product.getLastUpdateIntervalDays() < 7)
                throw new RuntimeException("⚠️ Product must be updated ≥ 7 days ago to join: " + product.getName());

            PlatformCampaignFlashSlot slot = null;
            LocalDateTime start = campaign.getStartTime();
            LocalDateTime end   = campaign.getEndTime();

            if (isFastSale) {
                if (item.getSlotId() == null)
                    throw new RuntimeException("❌ FAST_SALE requires slotId for product: " + product.getName());
                slot = flashSlotRepository.findById(item.getSlotId())
                        .orElseThrow(() -> new RuntimeException("❌ Slot not found: " + item.getSlotId()));
                if (!slot.getCampaign().getId().equals(campaignId))
                    throw new RuntimeException("⚠️ Slot not in this campaign");
                start = slot.getOpenTime();
                end   = slot.getCloseTime();
            }

            // Tính discountedPrice
            BigDecimal original = product.getPrice();
            BigDecimal discounted = original;

            if (item.getType() == VoucherType.FIXED && item.getDiscountValue() != null) {
                discounted = original.subtract(item.getDiscountValue()).max(BigDecimal.ZERO);
            } else if (item.getType() == VoucherType.PERCENT && item.getDiscountPercent() != null) {
                BigDecimal cut = original.multiply(BigDecimal.valueOf(item.getDiscountPercent())).divide(BigDecimal.valueOf(100));
                if (item.getMaxDiscountValue() != null) {
                    cut = cut.min(item.getMaxDiscountValue());
                }
                discounted = original.subtract(cut).max(BigDecimal.ZERO);
            } else if (item.getType() == VoucherType.SHIPPING) {
                // shipping voucher không ảnh hưởng product price — vẫn để original
            } else {
                throw new RuntimeException("❌ Invalid voucher config for product: " + product.getName());
            }

            // Không trùng đăng ký
            if (campaignProductRepository.existsByCampaign_IdAndProduct_ProductId(campaignId, product.getProductId()))
                throw new RuntimeException("⚠️ Product already joined campaign: " + product.getName());

            PlatformCampaignProduct entity = PlatformCampaignProduct.builder()
                    .campaign(campaign)
                    .store(store)
                    .product(product)
                    .flashSlot(slot)

                    .type(item.getType())
                    .discountValue(item.getDiscountValue())
                    .discountPercent(item.getDiscountPercent())
                    .maxDiscountValue(item.getMaxDiscountValue())
                    .minOrderValue(item.getMinOrderValue())

                    .totalVoucherIssued(item.getTotalVoucherIssued())
                    .totalUsageLimit(item.getTotalUsageLimit())
                    .usagePerUser(item.getUsagePerUser())
                    .remainingUsage(item.getTotalUsageLimit())

                    .originalPrice(original)
                    .discountedPrice(discounted)

                    .startTime(start)
                    .endTime(end)

                    // FAST_SALE: auto active ngay khi đăng ký; MEGA_SALE cũng ACTIVE
                    .status(VoucherStatus.ACTIVE)
                    .approved(true)
                    .approvedAt(LocalDateTime.now())
                    .registeredAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            toSave.add(entity);
        }

        campaignProductRepository.saveAll(toSave);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Joined campaign successfully", toSave.size()));
    }

    // =====================================================
    // 3) GET FAST_SALE CAMPAIGNS (kèm slots) THEO BỘ LỌC
    // =====================================================
    @Override
    public ResponseEntity<BaseResponse> getFastSaleCampaigns(String status, LocalDateTime start, LocalDateTime end) {
        // chỉ lấy FAST_SALE
        List<PlatformCampaign> list = campaignRepository.findAll().stream()
                .filter(c -> c.getCampaignType() == CampaignType.FAST_SALE)
                .filter(c -> status == null || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> start == null || !c.getStartTime().isBefore(start))
                .filter(c -> end == null || !c.getEndTime().isAfter(end))
                .toList();

        List<CampaignWithSlotsResponse> res = list.stream().map(c ->
                CampaignWithSlotsResponse.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .name(c.getName())
                        .description(c.getDescription())
                        .campaignType(c.getCampaignType())
                        .badgeLabel(c.getBadgeLabel())
                        .badgeColor(c.getBadgeColor())
                        .badgeIconUrl(c.getBadgeIconUrl())
                        .allowRegistration(c.getAllowRegistration())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .status(c.getStatus())
                        .slots(flashSlotRepository.findAllByCampaign_Id(c.getId()).stream()
                                .map(sl -> CampaignWithSlotsResponse.SlotDto.builder()
                                        .id(sl.getId())
                                        .openTime(sl.getOpenTime())
                                        .closeTime(sl.getCloseTime())
                                        .status(sl.getStatus())
                                        .build())
                                .toList())
                        .build()
        ).toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 Fast Sale campaigns", res));
    }

    // =====================================================
    // 4) GET SẢN PHẨM THEO SLOT (EXPIRED / ONGOING / UPCOMING)
    // =====================================================
    @Override
    public ResponseEntity<BaseResponse> getSlotProducts(UUID campaignId, UUID slotId, String timeFilter) {
        LocalDateTime now = LocalDateTime.now();

        List<PlatformCampaignProduct> list = campaignProductRepository.filter(
                campaignId,
                slotId,
                null,
                null,
                null
        );

        List<PlatformCampaignProduct> filtered = switch (timeFilter == null ? "" : timeFilter.toUpperCase()) {
            case "EXPIRED" -> list.stream().filter(p -> p.getEndTime() != null && p.getEndTime().isBefore(now)).toList();
            case "ONGOING" -> list.stream().filter(p ->
                    p.getStartTime() != null && p.getEndTime() != null &&
                    ( !p.getStartTime().isAfter(now) && !p.getEndTime().isBefore(now) )
            ).toList();
            case "UPCOMING" -> list.stream().filter(p -> p.getStartTime() != null && p.getStartTime().isAfter(now)).toList();
            default -> list; // all
        };

        var res = SlotProductsResponse.builder()
                .campaignId(campaignId)
                .slotId(slotId)
                .timeFilter(timeFilter)
                .items(filtered.stream().map(p ->
                        SlotProductsResponse.Item.builder()
                                .campaignProductId(p.getId())
                                .productId(p.getProduct().getProductId())
                                .productName(p.getProduct().getName())
                                .brandName(p.getProduct().getBrandName())
                                .originalPrice(p.getOriginalPrice())
                                .discountedPrice(p.getDiscountedPrice())
                                .type(p.getType())
                                .discountValue(p.getDiscountValue())
                                .discountPercent(p.getDiscountPercent())
                                .maxDiscountValue(p.getMaxDiscountValue())
                                .minOrderValue(p.getMinOrderValue())
                                .totalVoucherIssued(p.getTotalVoucherIssued())
                                .totalUsageLimit(p.getTotalUsageLimit())
                                .usagePerUser(p.getUsagePerUser())
                                .remainingUsage(p.getRemainingUsage())
                                .startTime(p.getStartTime())
                                .endTime(p.getEndTime())
                                .status(p.getStatus())
                                .build()
                ).toList())
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "🧾 Slot products", res));
    }

    // =====================================================
    // 5) SCHEDULER: BẬT/TẮT SLOT & UPDATE PRODUCT STATUS
    // =====================================================
    @Override
    @Transactional
    public void tickFlashSlots() {
        LocalDateTime now = LocalDateTime.now();

        // a) Mở slot: PENDING -> ACTIVE nếu now ∈ [open, close]
        List<PlatformCampaignFlashSlot> toOpen = flashSlotRepository.findAllByStatus(SlotStatus.PENDING)
                .stream().filter(s -> !now.isBefore(s.getOpenTime()) && !now.isAfter(s.getCloseTime()))
                .toList();
        if (!toOpen.isEmpty()) {
            flashSlotRepository.bulkUpdateStatus(toOpen.stream().map(PlatformCampaignFlashSlot::getId).toList(), SlotStatus.ACTIVE);
        }

        // b) Đóng slot: ACTIVE -> CLOSED nếu now > close
        List<PlatformCampaignFlashSlot> toClose = flashSlotRepository.findAllByStatus(SlotStatus.ACTIVE)
                .stream().filter(s -> now.isAfter(s.getCloseTime()))
                .toList();
        if (!toClose.isEmpty()) {
            flashSlotRepository.bulkUpdateStatus(toClose.stream().map(PlatformCampaignFlashSlot::getId).toList(), SlotStatus.CLOSED);
            // đồng thời EXPIRE toàn bộ product thuộc slot vừa đóng
            campaignProductRepository.bulkUpdateStatusBySlot(
                    toClose.stream().map(PlatformCampaignFlashSlot::getId).toList(),
                    VoucherStatus.EXPIRED
            );
        }

        // c) Phòng hờ: mọi product có endTime < now → EXPIRED
        List<PlatformCampaignProduct> all = campaignProductRepository.findAll();
        List<PlatformCampaignProduct> expire = all.stream()
                .filter(p -> p.getEndTime() != null && p.getEndTime().isBefore(now))
                .filter(p -> p.getStatus() != VoucherStatus.EXPIRED)
                .toList();
        expire.forEach(p -> p.setStatus(VoucherStatus.EXPIRED));
        if (!expire.isEmpty()) campaignProductRepository.saveAll(expire);
    }

    // =====================================================
    // Helpers
    // =====================================================
    private Store getCurrentStore() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = auth != null ? auth.getName() : null;
        if (principal == null) throw new RuntimeException("❌ Not authenticated");
        String email = principal.contains(":") ? principal.split(":")[0] : principal;
        return storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user (email=" + email + ")"));
    }

    @Override
public ResponseEntity<BaseResponse> getAllCampaigns(String type, String status,
                                                    LocalDateTime start, LocalDateTime end) {
    CampaignType typeEnum = null;
    VoucherStatus statusEnum = null;

    if (type != null && !type.isBlank()) {
        try {
            typeEnum = CampaignType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ Invalid type: must be MEGA_SALE or FAST_SALE");
        }
    }

    if (status != null && !status.isBlank()) {
        try {
            statusEnum = VoucherStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ Invalid status: must be ACTIVE / CLOSED / DRAFT");
        }
    }

   CampaignType finalTypeEnum = typeEnum;
VoucherStatus finalStatusEnum = statusEnum;

List<PlatformCampaign> campaigns = campaignRepository.findAll().stream()
        .filter(c -> (finalTypeEnum == null || c.getCampaignType().equals(finalTypeEnum)))
        .filter(c -> (finalStatusEnum == null || c.getStatus().equals(finalStatusEnum)))
        .filter(c -> (start == null || !c.getStartTime().isBefore(start)))
        .filter(c -> (end == null || !c.getEndTime().isAfter(end)))
        .toList();
    if (campaigns.isEmpty()) {
        return ResponseEntity.ok(new BaseResponse<>(404, "⚠️ Không có campaign phù hợp", List.of()));
    }

    // ✅ Build response
    var responseList = campaigns.stream().map(campaign -> {
        Map<String, Object> campaignMap = new LinkedHashMap<>();
        campaignMap.put("id", campaign.getId());
        campaignMap.put("code", campaign.getCode());
        campaignMap.put("name", campaign.getName());
        campaignMap.put("description", campaign.getDescription());
        campaignMap.put("type", campaign.getCampaignType());
        campaignMap.put("status", campaign.getStatus());
        campaignMap.put("startTime", campaign.getStartTime());
        campaignMap.put("endTime", campaign.getEndTime());
        campaignMap.put("allowRegistration", campaign.getAllowRegistration());
        campaignMap.put("badgeLabel", campaign.getBadgeLabel());
        campaignMap.put("badgeColor", campaign.getBadgeColor());
        campaignMap.put("badgeIconUrl", campaign.getBadgeIconUrl());

        // 🔹 Nếu là Fast Sale → lấy kèm slot
        if (campaign.getCampaignType() == CampaignType.FAST_SALE) {
            var slots = campaign.getFlashSlots().stream().map(slot -> Map.of(
                    "slotId", slot.getId(),
                    "openTime", slot.getOpenTime(),
                    "closeTime", slot.getCloseTime(),
                    "status", slot.getStatus()
            )).toList();
            campaignMap.put("flashSlots", slots);
        }

        return campaignMap;
    }).toList();

    return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy danh sách campaign thành công", responseList));
}

}
