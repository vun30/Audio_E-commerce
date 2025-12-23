// org.example.audio_ecommerce.service.Impl.PlatformCampaignServiceImpl
package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateOrUpdateCampaignRequest;
import org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest;
import org.example.audio_ecommerce.dto.request.RejectProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateCampaignRequest;
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
    private final PlatformCampaignStoreRepository campaignStoreRepository;

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

        //// === FIX TIMEZONE HERE (subtract 7 hours) ===
        LocalDateTime fixedStart = req.getStartTime().minusHours(0);
        LocalDateTime fixedEnd = req.getEndTime().minusHours(0);

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
                //// === USE FIXED TIME ===
                .startTime(fixedStart)
                .endTime(fixedEnd)
                //// ======================
                .status(VoucherStatus.DRAFT)
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
    public ResponseEntity<BaseResponse<CampaignResponse>> updateCampaign(UUID campaignId, UpdateCampaignRequest req) {

        // 1️⃣ Lấy campaign theo ID
        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign không tồn tại"));

        VoucherStatus oldStatus = campaign.getStatus();
        VoucherStatus newStatus = oldStatus;

        // 2️⃣ Cập nhật thông tin cơ bản (nếu có)
        if (req.getName() != null) campaign.setName(req.getName());
        if (req.getDescription() != null) campaign.setDescription(req.getDescription());
        //// === FIX TIMEZONE WHEN UPDATING ===
        if (req.getStartTime() != null)
            campaign.setStartTime(req.getStartTime());

        if (req.getEndTime() != null)
            campaign.setEndTime(req.getEndTime());
//// ==================================

        if (req.getAllowRegistration() != null) campaign.setAllowRegistration(req.getAllowRegistration());
        if (req.getBadgeLabel() != null) campaign.setBadgeLabel(req.getBadgeLabel());
        if (req.getBadgeColor() != null) campaign.setBadgeColor(req.getBadgeColor());
        if (req.getBadgeIconUrl() != null) campaign.setBadgeIconUrl(req.getBadgeIconUrl());
        if (req.getApprovalRule() != null) campaign.setApprovalRule(req.getApprovalRule());

        // 3️⃣ Nếu có yêu cầu cập nhật trạng thái campaign
        if (req.getStatus() != null) {
            try {
                newStatus = VoucherStatus.valueOf(req.getStatus().trim().toUpperCase());
                campaign.setStatus(newStatus);
                // ❌ Admin không được ACTIVE thủ công trước giờ start, chỉ scheduler được phép
                if (newStatus == VoucherStatus.ACTIVE && campaign.getStartTime().isAfter(LocalDateTime.now())) {
                    throw new RuntimeException("❌ ACTIVE chỉ scheduler tự bật khi tới startTime");
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("❌ Trạng thái không hợp lệ (DRAFT / ACTIVE / DISABLED / EXPIRED / APPROVE / CLOSED)");
            }
        }

        // 4️⃣ Nếu là FAST_SALE → cho phép update / thêm slot
        if (campaign.getCampaignType() == CampaignType.FAST_SALE && req.getFlashSlots() != null) {
            System.out.println("🕓 [Update] Bắt đầu xử lý slots cho campaign: " + campaign.getName());

            // Lấy toàn bộ slot hiện tại
            List<PlatformCampaignFlashSlot> existingSlots = flashSlotRepository.findAllByCampaign_Id(campaignId);
            Map<UUID, PlatformCampaignFlashSlot> slotMap = existingSlots.stream()
                    .collect(Collectors.toMap(PlatformCampaignFlashSlot::getId, s -> s));

            List<PlatformCampaignFlashSlot> toSave = new ArrayList<>();

            for (UpdateCampaignRequest.FlashSlotUpdateDto s : req.getFlashSlots()) {
                // CASE 1️⃣: Slot có ID → cập nhật slot cũ
                if (s.getId() != null && slotMap.containsKey(s.getId())) {
                    PlatformCampaignFlashSlot slot = slotMap.get(s.getId());

                    if (s.getOpenTime() != null) slot.setOpenTime(s.getOpenTime());
                    if (s.getCloseTime() != null) slot.setCloseTime(s.getCloseTime());

                    if (s.getStatus() != null) {
                        try {
                            SlotStatus newSlotStatus = SlotStatus.valueOf(s.getStatus().trim().toUpperCase());
                            slot.setStatus(newSlotStatus);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("❌ Slot status không hợp lệ (PENDING / ACTIVE / CLOSED / DISABLED)");
                        }
                    }

                    toSave.add(slot);
                    System.out.printf("✏️ Đã cập nhật slot [%s] %s → %s%n",
                            slot.getId(), slot.getOpenTime(), slot.getCloseTime());
                }
                // CASE 2️⃣: Slot không có ID → tạo mới
                else if (s.getId() == null) {
                    if (s.getOpenTime() == null || s.getCloseTime() == null || !s.getCloseTime().isAfter(s.getOpenTime())) {
                        throw new RuntimeException("❌ Invalid slot time (openTime < closeTime required)");
                    }

                    PlatformCampaignFlashSlot newSlot = PlatformCampaignFlashSlot.builder()
                            .campaign(campaign)
                            .openTime(s.getOpenTime())
                            .closeTime(s.getCloseTime())
                            .status(SlotStatus.PENDING)
                            .build();

                    toSave.add(newSlot);
                    System.out.printf("🆕 Đã thêm slot mới %s → %s%n",
                            newSlot.getOpenTime(), newSlot.getCloseTime());
                }
                // CASE 3️⃣: ID không tồn tại trong DB
                else {
                    throw new RuntimeException("⚠️ Slot ID không tồn tại: " + s.getId());
                }
            }

            flashSlotRepository.saveAll(toSave);
            System.out.println("✅ Hoàn tất cập nhật slot cho campaign.");
        }

        // 5️⃣ Nếu campaign chuyển sang DISABLED → vô hiệu hóa toàn bộ slot và product
        if (newStatus == VoucherStatus.DISABLED) {
            System.out.printf("⚠️ Campaign '%s' bị vô hiệu hoá → toàn bộ slot & product cũng DISABLED%n", campaign.getName());

            List<PlatformCampaignFlashSlot> slots = flashSlotRepository.findAllByCampaign_Id(campaign.getId());
            slots.forEach(slot -> slot.setStatus(SlotStatus.CLOSED));
            flashSlotRepository.saveAll(slots);

            List<PlatformCampaignProduct> products = campaignProductRepository.findAll().stream()
                    .filter(p -> p.getCampaign().getId().equals(campaignId))
                    .toList();

            products.forEach(p -> {
                p.setStatus(VoucherStatus.DISABLED);
                p.setApproved(false);
                p.setUpdatedAt(LocalDateTime.now());
            });

            campaignProductRepository.saveAll(products);
        }

        // 6️⃣ Nếu bật lại từ DISABLED → ACTIVE
        else if (oldStatus == VoucherStatus.DISABLED && newStatus == VoucherStatus.ACTIVE) {
            System.out.printf("🟢 Campaign '%s' được kích hoạt lại%n", campaign.getName());

            List<PlatformCampaignFlashSlot> slots = flashSlotRepository.findAllByCampaign_Id(campaign.getId());
            slots.forEach(slot -> {
                if (slot.getStatus() == SlotStatus.CLOSED) slot.setStatus(SlotStatus.PENDING);
            });
            flashSlotRepository.saveAll(slots);

            List<PlatformCampaignProduct> products = campaignProductRepository.findAll().stream()
                    .filter(p -> p.getCampaign().getId().equals(campaignId))
                    .toList();

            products.forEach(p -> {
                if (p.getStatus() == VoucherStatus.DISABLED) p.setStatus(VoucherStatus.DRAFT);
                p.setUpdatedAt(LocalDateTime.now());
            });
            campaignProductRepository.saveAll(products);
        }

        // 7️⃣ Lưu cập nhật campaign
        campaignRepository.save(campaign);

        // 8️⃣ Build response trả về dạng DTO
        CampaignResponse response = CampaignResponse.builder()
                .id(campaign.getId())
                .code(campaign.getCode())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .campaignType(campaign.getCampaignType())
                .badgeLabel(campaign.getBadgeLabel())
                .badgeColor(campaign.getBadgeColor())
                .badgeIconUrl(campaign.getBadgeIconUrl())
                .status(campaign.getStatus())
                .allowRegistration(campaign.getAllowRegistration())
                .approvalRule(campaign.getApprovalRule())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .flashSlots(
                        flashSlotRepository.findAllByCampaign_Id(campaign.getId())
                                .stream()
                                .map(slot -> CampaignResponse.FlashSlotDto.builder()
                                        .id(slot.getId())
                                        .openTime(slot.getOpenTime())
                                        .closeTime(slot.getCloseTime())
                                        .status(slot.getStatus())
                                        .build())
                                .toList()
                )
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Cập nhật campaign & slot thành công", response));
    }


    @Override
    @Transactional
    public ResponseEntity<BaseResponse> joinCampaign(UUID campaignId, CampaignProductRegisterRequest req) {
        Store store = getCurrentStore();

        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign not found"));

        ensureStoreJoinedCampaign(campaign, store);

        LocalDateTime now = LocalDateTime.now();

        if (campaign.getStatus() != VoucherStatus.ONOPEN)
            throw new RuntimeException("🚫 Campaign must be in ONOPEN status to allow registration");

        if (campaign.getStartTime() != null && !now.isBefore(campaign.getStartTime()))
            throw new RuntimeException("🚫 Campaign has already started or expired — cannot join");

        if (campaign.getEndTime() != null && now.isAfter(campaign.getEndTime()))
            throw new RuntimeException("🚫 Campaign has already ended — cannot join");

        if (!Boolean.TRUE.equals(campaign.getAllowRegistration()))
            throw new RuntimeException("🚫 Registration disabled for this campaign");

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

            PlatformCampaignFlashSlot slot = null;
            LocalDateTime start = campaign.getStartTime();
            LocalDateTime end = campaign.getEndTime();

            if (isFastSale) {
                if (item.getSlotId() == null)
                    throw new RuntimeException("❌ FAST_SALE requires slotId for product: " + product.getName());
                slot = flashSlotRepository.findById(item.getSlotId())
                        .orElseThrow(() -> new RuntimeException("❌ Slot not found: " + item.getSlotId()));
                if (!slot.getCampaign().getId().equals(campaignId))
                    throw new RuntimeException("⚠️ Slot not in this campaign");
                start = slot.getOpenTime();
                end = slot.getCloseTime();
            }

            // ✅ RULE mới: product không được overlap thời gian với campaign khác
            validateProductNotOverlappingCampaign(product, start, end);

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
                    .startTime(start)
                    .endTime(end)
                    .status(VoucherStatus.DRAFT)
                    .approved(false)
                    .registeredAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            toSave.add(entity);
        }

        campaignProductRepository.saveAll(toSave);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Joined campaign successfully in DRAFT mode", toSave.size()));
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
            case "EXPIRED" ->
                    list.stream().filter(p -> p.getEndTime() != null && p.getEndTime().isBefore(now)).toList();
            case "ONGOING" -> list.stream().filter(p ->
                    p.getStartTime() != null && p.getEndTime() != null &&
                            (!p.getStartTime().isAfter(now) && !p.getEndTime().isBefore(now))
            ).toList();
            case "UPCOMING" ->
                    list.stream().filter(p -> p.getStartTime() != null && p.getStartTime().isAfter(now)).toList();
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

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> getCampaignProducts(
            UUID campaignId,
            UUID storeId,
            String status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        // 🔍 1) Lấy thông tin campaign
        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign không tồn tại"));

        // 🔍 2) Parse trạng thái nếu có
        VoucherStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = VoucherStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("❌ Trạng thái không hợp lệ (DRAFT / ACTIVE / EXPIRED)");
            }
        }

        // 🔍 3) Lọc danh sách sản phẩm từ repo
        List<PlatformCampaignProduct> list = campaignProductRepository.filterProducts(
                campaignId, storeId, statusEnum, from, to
        );

        // ⚙️ 4) Map dữ liệu an toàn (null-safe + tránh lỗi lazy load)
        List<Map<String, Object>> data = list.stream()
                .map(p -> {
                    Product prod = p.getProduct();
                    Store store = p.getStore();

                    Map<String, Object> map = Map.ofEntries(
                            Map.entry("campaignProductId", p.getId()),
                            Map.entry("productId", prod != null ? prod.getProductId() : null),
                            Map.entry("productName", prod != null ? prod.getName() : "(Unknown Product)"),
                            Map.entry("storeId", store != null ? store.getStoreId() : null),
                            Map.entry("storeName", store != null ? store.getStoreName() : "(Unknown Store)"),
                            Map.entry("status", p.getStatus()),
                            Map.entry("approved", p.getApproved()),
                            Map.entry("createdAt", p.getCreatedAt()),
                            Map.entry("approvedAt", p.getApprovedAt()),
                            Map.entry("discountType", p.getType()),
                            Map.entry("discountValue", p.getDiscountValue()),
                            Map.entry("discountPercent", p.getDiscountPercent())
                    );
                    return (Map<String, Object>) map; // ✅ ép kiểu an toàn
                })
                .toList();

        // ✅ 5) Build response
        BaseResponse<List<Map<String, Object>>> response = BaseResponse.<List<Map<String, Object>>>builder()
                .status(200)
                .message("✅ Danh sách sản phẩm tham gia campaign: " + campaign.getName())
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }


    @Override
    @Transactional
    public ResponseEntity<BaseResponse> approveCampaignProducts(UUID campaignId, List<UUID> campaignProductIds) {
        if (campaignProductIds == null || campaignProductIds.isEmpty()) {
            throw new RuntimeException("❌ Danh sách campaignProductIds không được trống");
        }

        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign không tồn tại"));

        List<PlatformCampaignProduct> products = campaignProductRepository.findAllById(campaignProductIds);

        if (products.isEmpty()) {
            throw new RuntimeException("⚠️ Không tìm thấy sản phẩm tương ứng với campaignProductIds");
        }

        List<PlatformCampaignProduct> draftProducts = products.stream()
                .filter(p -> p.getStatus() == VoucherStatus.DRAFT)
                .toList();

        if (draftProducts.isEmpty()) {
            return ResponseEntity.ok(new BaseResponse<>(200, "⚠️ Không có sản phẩm nào ở trạng thái DRAFT để duyệt", List.of()));
        }

        LocalDateTime now = LocalDateTime.now();

        // ✅ Re-check overlap trước khi approve
        for (PlatformCampaignProduct p : draftProducts) {
            Product product = p.getProduct();
            LocalDateTime start = p.getStartTime();
            LocalDateTime end = p.getEndTime();

            validateProductNotOverlappingCampaign(product, start, end);
        }

        // 4️⃣ Cập nhật trạng thái: DRAFT → APPROVE
        draftProducts.forEach(p -> {
            p.setApproved(true);
            p.setApprovedAt(now);
            p.setUpdatedAt(now);
            p.setStatus(VoucherStatus.APPROVE);
        });

        campaignProductRepository.saveAll(draftProducts);

        // ✅ update storeCampaign approve
        draftProducts.forEach(p -> {
            markStoreCampaignApproved(p.getCampaign(), p.getStore());
        });

        List<Map<String, Object>> data = draftProducts.stream().map(p -> {
            Product prod = p.getProduct();
            Store store = p.getStore();

            return Map.<String, Object>of(
                    "campaignProductId", p.getId(),
                    "productId", prod != null ? prod.getProductId() : null,
                    "productName", prod != null ? prod.getName() : "(Unknown Product)",
                    "storeId", store != null ? store.getStoreId() : null,
                    "storeName", store != null ? store.getStoreName() : "(Unknown Store)",
                    "oldStatus", "DRAFT",
                    "newStatus", "APPROVE",
                    "approved", p.getApproved(),
                    "approvedAt", p.getApprovedAt()
            );
        }).toList();

        return ResponseEntity.ok(new BaseResponse<>(
                200,
                "✅ Đã duyệt " + draftProducts.size() + " sản phẩm (DRAFT → APPROVE) trong campaign " + campaign.getName(),
                data
        ));
    }


    @Override
    @Transactional
    public ResponseEntity<BaseResponse> updateCampaignProductStatus(UUID campaignId, String newStatus, List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("❌ Danh sách productIds không được trống");
        }

        // 🧩 Kiểm tra campaign tồn tại
        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign không tồn tại"));

        // 🧩 Parse trạng thái đích
        VoucherStatus targetStatus;
        try {
            targetStatus = VoucherStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ Trạng thái không hợp lệ (chỉ cho phép: DRAFT, ACTIVE, EXPIRED, DISABLED,ONOPEN)");
        }

        // 🧩 Lấy danh sách sản phẩm bằng query mới
        List<PlatformCampaignProduct> products = campaignProductRepository
                .findByCampaignAndProducts(campaignId, productIds);

        if (products.isEmpty()) {
            throw new RuntimeException("⚠️ Không tìm thấy sản phẩm phù hợp để cập nhật trạng thái");
        }

        LocalDateTime now = LocalDateTime.now();

        // 🔁 Cập nhật trạng thái
        products.forEach(p -> {
            VoucherStatus oldStatus = p.getStatus();
            p.setStatus(targetStatus);
            p.setUpdatedAt(now);

            if (targetStatus == VoucherStatus.ACTIVE) {
                p.setApproved(true);
                p.setApprovedAt(now);
            } else if (targetStatus == VoucherStatus.DISABLED) {
                p.setApproved(false);
            }

            // ✅ Nếu chuyển EXPIRED → ACTIVE, reset lại remainingUsage nếu muốn (tuỳ logic)
        });

        campaignProductRepository.saveAll(products);

        // ✅ Response
        List<Map<String, Object>> response = products.stream().map(p -> {
            Product prod = p.getProduct();
            Store store = p.getStore();
            return Map.<String, Object>of(
                    "productId", prod != null ? prod.getProductId() : null,
                    "productName", prod != null ? prod.getName() : "(Unknown Product)",
                    "storeName", store != null ? store.getStoreName() : "(Unknown Store)",
                    "oldStatus", p.getStatus().name(),
                    "newStatus", targetStatus.name(),
                    "updatedAt", now
            );
        }).toList();

        return ResponseEntity.ok(new BaseResponse<>(
                200,
                "✅ Đã chuyển " + products.size() + " sản phẩm sang trạng thái " + targetStatus,
                response
        ));
    }

    @Override
    @Transactional
    public void tickAllCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        System.out.println("🕒 [Scheduler] tickAllCampaigns() chạy lúc: " + now);

        // ==========================================================
        // 1) UPDATE CAMPAIGN STATUS
        // ==========================================================
        List<PlatformCampaign> campaigns = campaignRepository.findAll();

        for (PlatformCampaign c : campaigns) {
            VoucherStatus oldStatus = c.getStatus();

            if (c.getStartTime() != null && now.isBefore(c.getStartTime())) {
                continue;
            }

            // ACTIVE only khi trước đó là ONOPEN hoặc APPROVE  // FIXED
            if (!now.isBefore(c.getStartTime()) && !now.isAfter(c.getEndTime())) {
                if (oldStatus == VoucherStatus.ONOPEN || oldStatus == VoucherStatus.APPROVE) { // FIXED
                    c.setStatus(VoucherStatus.ACTIVE);
                    System.out.printf("📢 Campaign '%s' chuyển từ %s → ACTIVE%n", c.getName(), oldStatus);
                }
            }

            // EXPIRED
            if (c.getEndTime() != null && now.isAfter(c.getEndTime())) {
                if (c.getStatus() != VoucherStatus.EXPIRED) {
                    c.setStatus(VoucherStatus.EXPIRED);
                    System.out.printf("📢 Campaign '%s' chuyển từ %s → EXPIRED%n", c.getName(), oldStatus);
                }
            }
        }
        campaignRepository.saveAll(campaigns);


        // ==========================================================
        // 2) SLOT UPDATE (FAST SALE)
        // ==========================================================
        List<PlatformCampaignFlashSlot> slots = flashSlotRepository.findAll();

        for (PlatformCampaignFlashSlot s : slots) {
            PlatformCampaign campaign = s.getCampaign();
            if (campaign == null || campaign.getStatus() != VoucherStatus.ACTIVE) continue;

            SlotStatus oldStatus = s.getStatus();

            if (!now.isBefore(s.getOpenTime()) && !now.isAfter(s.getCloseTime())) {
                if (s.getStatus() == SlotStatus.PENDING) {
                    s.setStatus(SlotStatus.ACTIVE);
                    System.out.printf("🟢 Slot [%s] của Campaign '%s' chuyển từ %s → ACTIVE%n",
                            s.getId(), campaign.getName(), oldStatus);
                }
            } else if (now.isAfter(s.getCloseTime())) {
                s.setStatus(SlotStatus.CLOSED);
            } else if (now.isBefore(s.getOpenTime())) {
                s.setStatus(SlotStatus.PENDING);
            }
        }
        flashSlotRepository.saveAll(slots);


        // ==========================================================
        // 3) PRODUCT IN CAMPAIGN UPDATE
        // ==========================================================
        List<PlatformCampaignProduct> products = campaignProductRepository.findAll();

        for (PlatformCampaignProduct p : products) {

            PlatformCampaign campaign = p.getCampaign();
            PlatformCampaignFlashSlot slot = p.getFlashSlot();
            if (campaign == null) continue;

            // FAST SALE
            if (slot != null) {

                // FIXED: APPROVE cũng được chuyển ACTIVE
                if (campaign.getStatus() == VoucherStatus.ACTIVE &&
                        !now.isBefore(slot.getOpenTime()) &&
                        (p.getStatus() == VoucherStatus.ONOPEN || p.getStatus() == VoucherStatus.APPROVE) && // FIXED
                        !now.isAfter(slot.getCloseTime())) {
                    p.setStatus(VoucherStatus.ACTIVE);
                }

                if (now.isAfter(slot.getCloseTime()) ||
                        campaign.getStatus() == VoucherStatus.EXPIRED ||
                        (p.getEndTime() != null && now.isAfter(p.getEndTime()))) {
                    p.setStatus(VoucherStatus.EXPIRED);
                }
            }
            // MEGA_SALE
            else {

                // FIXED: APPROVE cũng auto ACTIVE nếu đến giờ chạy
                if (campaign.getStatus() == VoucherStatus.ACTIVE &&
                        now.isAfter(campaign.getStartTime()) &&
                        now.isBefore(campaign.getEndTime()) &&
                        (p.getStatus() == VoucherStatus.ONOPEN || p.getStatus() == VoucherStatus.APPROVE)) { // FIXED
                    p.setStatus(VoucherStatus.ACTIVE);
                }

                if (campaign.getStatus() == VoucherStatus.EXPIRED ||
                        (p.getEndTime() != null && now.isAfter(p.getEndTime()))) {
                    p.setStatus(VoucherStatus.EXPIRED);
                }
            }
        }

        campaignProductRepository.saveAll(products);
    }


    @Override
    public ResponseEntity<BaseResponse> getCampaignProductOverviewFiltered(
            String type,
            String status,
            UUID storeId,
            UUID campaignId,   // ✅ thêm tham số campaignId
            int page,
            int size
    ) {
        var typeEnum = (type != null) ? CampaignType.valueOf(type.toUpperCase()) : null;
        var statusEnum = (status != null) ? VoucherStatus.valueOf(status.toUpperCase()) : null;

        // ✅ Truyền thêm campaignId vào repo filter (nếu bạn đã update query)
        List<PlatformCampaignProduct> all = campaignProductRepository
                .filterCampaignProducts(typeEnum, statusEnum, storeId, campaignId);

        // Nếu repository chưa có campaignId, có thể lọc thủ công như sau:
        if (campaignId != null) {
            all = all.stream()
                    .filter(p -> p.getCampaign() != null && campaignId.equals(p.getCampaign().getId()))
                    .toList();
        }

        // ✅ Nhóm theo campaignId
        Map<UUID, List<PlatformCampaignProduct>> grouped =
                all.stream().collect(Collectors.groupingBy(p -> p.getCampaign().getId()));

        // ✅ Duyệt từng campaign để build JSON
        List<CampaignProductOverviewResponse> campaigns = grouped.entrySet().stream().map(entry -> {
            PlatformCampaign campaign = entry.getValue().get(0).getCampaign();

            List<CampaignProductOverviewResponse.ProductDto> productDtos = entry.getValue().stream().map(p -> {
                Product product = p.getProduct();
                Store store = p.getStore();

                var builder = CampaignProductOverviewResponse.ProductDto.builder()
                        .campaignProductId(p.getId()) // id bảng trung gian
                        .productId(product.getProductId())
                        .productName(product.getName())
                        .productImage(
                                (product.getImages() != null && !product.getImages().isEmpty())
                                        ? product.getImages().get(0)
                                        : null
                        )
                        .storeId(store.getStoreId())
                        .storeName(store.getStoreName());

                // 🔹 MEGA_SALE → 1 voucher duy nhất
                if (campaign.getCampaignType() == CampaignType.MEGA_SALE) {
                    builder.voucher(CampaignProductOverviewResponse.VoucherDto.builder()
                            .type(p.getType().name())
                            .discountValue(p.getDiscountValue())
                            .discountPercent(p.getDiscountPercent())
                            .maxDiscountValue(p.getMaxDiscountValue())
                            .minOrderValue(p.getMinOrderValue())
                            .status(p.getStatus().name())
                            .startTime(p.getStartTime())
                            .endTime(p.getEndTime())
                            .build());
                }

                // 🔹 FAST_SALE → nhiều slot
                else if (campaign.getCampaignType() == CampaignType.FAST_SALE) {
                    List<CampaignProductOverviewResponse.FlashSlotDto> slots =
                            entry.getValue().stream()
                                    .filter(x -> x.getProduct().getProductId().equals(product.getProductId()))
                                    .filter(x -> x.getFlashSlot() != null)
                                    .map(x -> {
                                        PlatformCampaignFlashSlot s = x.getFlashSlot();
                                        return CampaignProductOverviewResponse.FlashSlotDto.builder()
                                                .slotId(s.getId())
                                                .openTime(s.getOpenTime())
                                                .closeTime(s.getCloseTime())
                                                .status(s.getStatus().name())
                                                .voucher(CampaignProductOverviewResponse.VoucherDto.builder()
                                                        .type(x.getType().name())
                                                        .discountValue(x.getDiscountValue())
                                                        .discountPercent(x.getDiscountPercent())
                                                        .maxDiscountValue(x.getMaxDiscountValue())
                                                        .minOrderValue(x.getMinOrderValue())

                                                        // ✅ thêm phần này
                                                        .status(x.getStatus().name())
                                                        .startTime(x.getStartTime())
                                                        .endTime(x.getEndTime())
                                                        // ====================

                                                        .build())
                                                .build();
                                    }).toList();
                    builder.flashSaleSlots(slots);
                }

                return builder.build();
            }).toList();

            return CampaignProductOverviewResponse.builder()
                    .campaignId(campaign.getId())
                    .campaignName(campaign.getName())
                    .campaignType(campaign.getCampaignType().name())
                    .startTime(campaign.getStartTime())
                    .endTime(campaign.getEndTime())
                    .badgeLabel(campaign.getBadgeLabel())
                    .badgeColor(campaign.getBadgeColor())
                    .badgeIconUrl(campaign.getBadgeIconUrl())
                    .products(productDtos)
                    .build();
        }).toList();

        // ✅ Phân trang
        int from = page * size;
        int to = Math.min(from + size, campaigns.size());
        List<CampaignProductOverviewResponse> paged = campaigns.subList(Math.min(from, campaigns.size()), to);

        Map<String, Object> result = Map.of(
                "page", page,
                "size", size,
                "totalCampaigns", campaigns.size(),
                "data", paged
        );

        return ResponseEntity.ok(BaseResponse.success("✅ Danh sách sản phẩm theo loại chiến dịch (filtered)", result));
    }


    @Override
    @Transactional
    public ResponseEntity<BaseResponse> updateCampaignStatus(UUID campaignId, String newStatus) {

        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign not found"));

        VoucherStatus target;
        try {
            target = VoucherStatus.valueOf(newStatus.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("❌ Invalid status: {ONOPEN,DISABLED}");
        }

        VoucherStatus old = campaign.getStatus();

        // RULE
        // 1) ADMIN ko được chuyển sang ACTIVE
        if (target == VoucherStatus.ACTIVE) {
            throw new RuntimeException("❌ ACTIVE chỉ scheduler tự bật đúng giờ startTime");
        }

        // 2) Không cho EXPIRED thủ công
        if (target == VoucherStatus.EXPIRED) {
            throw new RuntimeException("❌ EXPIRED chỉ scheduler tự đóng khi qua endTime");
        }

        // 3) Chuyển từ DRAFT → ONOPEN (mở đăng ký store join)
        if (old == VoucherStatus.DRAFT && target == VoucherStatus.ONOPEN) {
            campaign.setStatus(VoucherStatus.ONOPEN);
        }
        // 4) DISABLED chỉ cho phép nếu campaign đang ở DRAFT hoặc ONOPEN
        else if (target == VoucherStatus.DISABLED) {
            if (old == VoucherStatus.EXPIRED) {
                throw new RuntimeException("⚠️ Không thể DISABLED campaign đã hết hạn (EXPIRED).");
            }
            if (old != VoucherStatus.DRAFT && old != VoucherStatus.ONOPEN) {
                throw new RuntimeException("⚠️ Không thể DISABLED campaign khi đang ở trạng thái: " + old + ". Chỉ cho phép khi DRAFT hoặc ONOPEN.");
            }
            campaign.setStatus(VoucherStatus.DISABLED);
        } else {
            throw new RuntimeException("⚠️ Transition not allowed: " + old + " → " + target);
        }

        campaignRepository.save(campaign);

        return ResponseEntity.ok(
                new BaseResponse<>(200,
                        "✅ Campaign status updated: " + old + " → " + target,
                        campaign.getStatus())
        );
    }

    private void validateProductNotOverlappingCampaign(Product product, LocalDateTime newStart, LocalDateTime newEnd) {

        List<PlatformCampaignProduct> existing = campaignProductRepository
                .findAllByProduct_ProductId(product.getProductId());

        for (PlatformCampaignProduct ex : existing) {

            // ❗Chỉ check nếu record kia đang ở trạng thái có hiệu lực
            // DRAFT thì bỏ qua
            if (ex.getStatus() == VoucherStatus.DRAFT || ex.getStatus() == VoucherStatus.DISABLED) {
                continue;
            }

            LocalDateTime exStart = ex.getStartTime();
            LocalDateTime exEnd = ex.getEndTime();

            if (exStart == null || exEnd == null) continue;

            // RULE 1: Overlap time hard block
            boolean overlap = !(newEnd.isBefore(exStart) || newStart.isAfter(exEnd));
            if (overlap) {
                throw new RuntimeException(
                        "🚫 Product " + product.getName()
                                + " đang tham gia campaign '" + ex.getCampaign().getName() + "' từ "
                                + exStart + " → " + exEnd + " (status: " + ex.getStatus() + ")"
                );
            }

            // RULE 3: FAST_SALE same day block nhưng cũng chỉ block khi campaign kia active/approve/onopen
            if (ex.getCampaign().getCampaignType() == CampaignType.FAST_SALE) {
                if (newStart.toLocalDate().isEqual(exStart.toLocalDate())) {
                    throw new RuntimeException(
                            "🚫 Product '" + product.getName() +
                                    "' đã tham gia Flash Sale khác trong ngày " + newStart.toLocalDate() +
                                    " (status: " + ex.getStatus() + "). Flash Sale là single shot ngày đó"
                    );
                }
            }
        }
    }

// org.example.audio_ecommerce.service.Impl.PlatformCampaignServiceImpl (thêm method)

    @Override
    public ResponseEntity<List<CampaignResponse>> getJoinedCampaignsByCampaignStatus(
            UUID storeId,
            String campaignStatus,
            Boolean storeApproved
    ) {
        if (storeId == null) throw new RuntimeException("❌ storeId required");

        VoucherStatus st = null;
        if (campaignStatus != null && !campaignStatus.isBlank()) {
            try {
                st = VoucherStatus.valueOf(campaignStatus.trim().toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException("❌ campaignStatus chỉ nhận: ONOPEN | ACTIVE | EXPIRED");
            }
        }

        List<PlatformCampaignStore> cs = campaignStoreRepository.findAllByStore_StoreId(storeId);

        LocalDateTime now = LocalDateTime.now();

        VoucherStatus finalSt = st;

        List<PlatformCampaign> campaigns = cs.stream()
                .filter(x -> x.getCampaign() != null)
                .filter(x -> storeApproved == null || Boolean.TRUE.equals(x.getApproved()) == storeApproved)
                .filter(x -> {
                    if (finalSt == null) return true;
                    PlatformCampaign c = x.getCampaign();
                    LocalDateTime start = c.getStartTime();
                    LocalDateTime end = c.getEndTime();
                    LocalDateTime nowL = now;

                    return switch (finalSt) {
                        case ONOPEN -> c.getStatus() == VoucherStatus.ONOPEN;
                        case ACTIVE -> c.getStatus() == VoucherStatus.ACTIVE
                                && start != null && end != null
                                && !nowL.isBefore(start) && !nowL.isAfter(end);
                        case EXPIRED -> c.getStatus() == VoucherStatus.EXPIRED
                                || (end != null && nowL.isAfter(end));
                        default -> false;
                    };
                })
                .map(PlatformCampaignStore::getCampaign)
                .distinct()
                .toList();

        List<CampaignResponse> resp = campaigns.stream()
                .map(c -> CampaignResponse.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .name(c.getName())
                        .description(c.getDescription())
                        .campaignType(c.getCampaignType())
                        .badgeLabel(c.getBadgeLabel())
                        .badgeColor(c.getBadgeColor())
                        .badgeIconUrl(c.getBadgeIconUrl())
                        .status(c.getStatus())
                        .allowRegistration(c.getAllowRegistration())
                        .approvalRule(c.getApprovalRule())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .createdAt(c.getCreatedAt())
                        .flashSlots(
                                c.getFlashSlots() == null ? null :
                                        c.getFlashSlots().stream().map(s ->
                                                CampaignResponse.FlashSlotDto.builder()
                                                        .id(s.getId())
                                                        .openTime(s.getOpenTime())
                                                        .closeTime(s.getCloseTime())
                                                        .status(s.getStatus())
                                                        .build()
                                        ).toList()
                        )
                        .build()
                ).toList();

        return ResponseEntity.ok(resp);
    }

    private void ensureStoreJoinedCampaign(PlatformCampaign campaign, Store store) {

        var exist = campaignStoreRepository
                .findByCampaign_IdAndStore_StoreId(campaign.getId(), store.getStoreId())
                .orElse(null);

        if (exist != null) return; // đã join rồi thì skip

        PlatformCampaignStore cs = PlatformCampaignStore.builder()
                .campaign(campaign)
                .store(store)
                .approved(false)     // default
                .registeredAt(LocalDateTime.now())
                .build();

        campaignStoreRepository.save(cs);
    }

    private void markStoreCampaignApproved(PlatformCampaign campaign, Store store) {

        PlatformCampaignStore cs = campaignStoreRepository
                .findByCampaign_IdAndStore_StoreId(campaign.getId(), store.getStoreId())
                .orElse(null);

        if (cs == null) return; // an toàn, ideally không xảy ra

        // đã approved trước đó thì update lastUpdateAt thôi, ko đổi approved nữa
        if (Boolean.TRUE.equals(cs.getApproved())) {
            cs.setLastUpdateAt(LocalDateTime.now());
            campaignStoreRepository.save(cs);
            return;
        }

        cs.setApproved(true);
        cs.setApprovedAt(LocalDateTime.now());
        cs.setLastUpdateAt(LocalDateTime.now());  // <--- thêm dòng này

        campaignStoreRepository.save(cs);
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> rejectCampaignProducts(
            UUID campaignId,
            RejectProductRequest req
    ) {
        if (req == null || req.getCampaignProductIds() == null || req.getCampaignProductIds().isEmpty()) {
            throw new RuntimeException("❌ Danh sách campaignProductIds không được trống");
        }

        PlatformCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("❌ Campaign không tồn tại"));

        List<PlatformCampaignProduct> products = campaignProductRepository.findAllById(req.getCampaignProductIds());
        if (products.isEmpty()) {
            throw new RuntimeException("⚠️ Không tìm thấy sản phẩm tương ứng với campaignProductIds");
        }

        LocalDateTime now = LocalDateTime.now();

        // 🔹 Nếu chỉ có 1 ID thì dùng reason trong request
        // 🔹 Nếu có nhiều ID mà chỉ gửi 1 reason thì dùng chung cho toàn bộ
        // 🔹 Nếu gửi kèm nhiều reason tương ứng -> dùng theo từng cặp (tùy kiểu FE gửi)
        String commonReason = req.getReason();
        Map<UUID, String> reasonMap = req.getReasonMap(); // option — nếu FE gửi dạng mapping

        List<Map<String, Object>> updatedList = new ArrayList<>();

        for (PlatformCampaignProduct p : products) {
            if (p.getStatus() == VoucherStatus.REJECTED || p.getStatus() == VoucherStatus.EXPIRED) continue;

            String appliedReason = (reasonMap != null && reasonMap.containsKey(p.getId()))
                    ? reasonMap.get(p.getId())
                    : commonReason;

            p.setStatus(VoucherStatus.REJECTED);
            p.setApproved(false);
            p.setApprovedAt(null);
            p.setReason(appliedReason);
            p.setUpdatedAt(now);

            campaignProductRepository.save(p);

            Product prod = p.getProduct();
            Store store = p.getStore();

            updatedList.add(Map.of(
                    "campaignProductId", p.getId(),
                    "productId", prod != null ? prod.getProductId() : null,
                    "productName", prod != null ? prod.getName() : "(Unknown Product)",
                    "storeName", store != null ? store.getStoreName() : "(Unknown Store)",
                    "status", "REJECTED",
                    "reason", appliedReason,
                    "updatedAt", now
            ));
        }

        return ResponseEntity.ok(new BaseResponse<>(
                200,
                "🚫 Đã từ chối " + updatedList.size() + " sản phẩm trong campaign '" + campaign.getName() + "'",
                updatedList
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> getCampaignProductDetails(UUID storeId, UUID campaignId, String status) {
        System.out.println("=== [DEBUG] getCampaignProductDetails CALLED ===");
        System.out.println("storeId = " + storeId);
        System.out.println("campaignId = " + campaignId);
        System.out.println("status = " + status);

        try {
            // =============================
            // 1️⃣ Parse Enum VoucherStatus
            // =============================
            VoucherStatus statusEnum = null;
            if (status != null && !status.isBlank()) {
                try {
                    statusEnum = VoucherStatus.valueOf(status.trim().toUpperCase());
                    System.out.println("Parsed statusEnum = " + statusEnum);
                } catch (IllegalArgumentException e) {
                    System.err.println("[ERROR] Invalid VoucherStatus: " + status);
                    throw new RuntimeException("❌ Invalid status: DRAFT / ACTIVE / APPROVE / EXPIRED / REJECTED / DISABLED");
                }
            }

            final VoucherStatus finalStatusEnum = statusEnum;

            System.out.println("=== [DEBUG] Fetching all campaign products... ===");
            List<PlatformCampaignProduct> list = campaignProductRepository.findAll();
            System.out.println("Total products loaded = " + list.size());

            // =============================
            // 2️⃣ Lọc dữ liệu theo điều kiện
            // =============================
            list = list.stream()
                    .filter(p -> {
                        boolean match = campaignId == null || (p.getCampaign() != null && p.getCampaign().getId().equals(campaignId));
                        if (!match) System.out.println("[Filter] Skip campaignId mismatch: " + p.getId());
                        return match;
                    })
                    .filter(p -> {
                        boolean match = storeId == null || (p.getStore() != null && p.getStore().getStoreId().equals(storeId));
                        if (!match) System.out.println("[Filter] Skip storeId mismatch: " + p.getId());
                        return match;
                    })
                    .filter(p -> {
                        boolean match = finalStatusEnum == null || (p.getStatus() != null && p.getStatus() == finalStatusEnum);
                        if (!match)
                            System.out.println("[Filter] Skip status mismatch: " + p.getId() + " status=" + p.getStatus());
                        return match;
                    })
                    .toList();

            System.out.println("After filter: " + list.size() + " products matched.");

            if (list.isEmpty()) {
                return ResponseEntity.ok(new BaseResponse<>(404, "⚠️ Không tìm thấy sản phẩm nào phù hợp với filter", List.of()));
            }

            // =============================
            // 3️⃣ Build JSON trả về (sử dụng LinkedHashMap tránh lỗi generic)
            // =============================
            System.out.println("=== [DEBUG] Building response... ===");
            List<Map<String, Object>> result = list.stream().map(p -> {
                Product prod = p.getProduct();
                Store store = p.getStore();
                PlatformCampaign campaign = p.getCampaign();
                PlatformCampaignFlashSlot slot = p.getFlashSlot();

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("campaignProductId", p.getId());
                map.put("campaignId", campaign != null ? campaign.getId() : null);
                map.put("campaignName", campaign != null ? campaign.getName() : null);
                map.put("campaignType", campaign != null ? campaign.getCampaignType() : null);
                map.put("storeId", store != null ? store.getStoreId() : null);
                map.put("storeName", store != null ? store.getStoreName() : null);
                map.put("productId", prod != null ? prod.getProductId() : null);
                map.put("productName", prod != null ? prod.getName() : null);
                map.put("brandName", prod != null ? prod.getBrandName() : null);
                map.put(
        "categories",
        prod != null && prod.getCategories() != null
                ? prod.getCategories().stream()
                    .map(Category::getName)
                    .toList()
                : List.of()
);

                map.put("discountType", p.getType());
                map.put("discountValue", p.getDiscountValue());
                map.put("discountPercent", p.getDiscountPercent());
                map.put("maxDiscountValue", p.getMaxDiscountValue());
                map.put("minOrderValue", p.getMinOrderValue());
                map.put("totalVoucherIssued", p.getTotalVoucherIssued());
                map.put("totalUsageLimit", p.getTotalUsageLimit());
                map.put("usagePerUser", p.getUsagePerUser());
                map.put("remainingUsage", p.getRemainingUsage());
                map.put("approved", p.getApproved());
                map.put("approvedAt", p.getApprovedAt());
                map.put("registeredAt", p.getRegisteredAt());
                map.put("status", p.getStatus());
                map.put("reason", p.getReason());
                map.put("startTime", p.getStartTime());
                map.put("endTime", p.getEndTime());
                map.put("slot", slot != null ? Map.of(
                        "slotId", slot.getId(),
                        "openTime", slot.getOpenTime(),
                        "closeTime", slot.getCloseTime(),
                        "slotStatus", slot.getStatus()
                ) : null);
                map.put("createdAt", p.getCreatedAt());
                map.put("updatedAt", p.getUpdatedAt());
                return map;
            }).toList();

            System.out.println("=== [DEBUG] Build success, returning response ===");
            return ResponseEntity.ok(new BaseResponse<>(200,
                    "✅ Lấy danh sách sản phẩm theo trạng thái " + (statusEnum != null ? statusEnum.name() : "ALL"),
                    result));

        } catch (Exception e) {
            System.err.println("=== [ERROR] Exception in getCampaignProductDetails ===");
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new BaseResponse<>(500, "❌ INTERNAL ERROR: " + e.getMessage(), null));
        }
    }

    @Override
@Transactional
public ResponseEntity<BaseResponse> withdrawCampaignProduct(UUID campaignProductId) {

    Store store = getCurrentStore(); // lấy store hiện tại

    PlatformCampaignProduct cp = campaignProductRepository.findById(campaignProductId)
            .orElseThrow(() -> new RuntimeException("❌ Campaign product not found"));

    PlatformCampaign campaign = cp.getCampaign();

    // 1️⃣ Rule: Campaign phải đang ở ONOPEN (mở đăng ký)
    if (campaign.getStatus() != VoucherStatus.ONOPEN) {
        throw new RuntimeException("🚫 Chỉ được rút sản phẩm khi campaign đang ONOPEN (mở đăng ký)");
    }

    // 2️⃣ Rule: Sản phẩm phải thuộc store hiện tại
    if (!cp.getStore().getStoreId().equals(store.getStoreId())) {
        throw new RuntimeException("🚫 Bạn không thể rút sản phẩm của store khác");
    }

    // 3️⃣ Rule: Chỉ được rút khi chưa duyệt (DRAFT)
    if (cp.getStatus() != VoucherStatus.DRAFT) {
        throw new RuntimeException("🚫 Chỉ được rút sản phẩm khi còn ở trạng thái DRAFT (chưa được duyệt)");
    }

    // 4️⃣ Xóa record luôn (Shopee cũng làm vậy)
    campaignProductRepository.delete(cp);

    return ResponseEntity.ok(
            new BaseResponse<>(
                    200,
                    "✅ Đã rút sản phẩm '" + cp.getProduct().getName() +
                            "' khỏi campaign '" + campaign.getName() + "'",
                    null
            )
    );
}


}

