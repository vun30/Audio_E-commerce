// service/Impl/VoucherServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.PlatformVoucherUse;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.PlatformVoucherUsageResponse;
import org.example.audio_ecommerce.dto.response.ShopVoucherUsageResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.PlatformCampaignProductUsageRepository;
import org.example.audio_ecommerce.repository.ShopVoucherRepository;
import org.example.audio_ecommerce.repository.ShopVoucherUsageRepository;
import org.example.audio_ecommerce.service.VoucherService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {
    private final ShopVoucherRepository voucherRepo;
    private final PlatformCampaignProductRepository campaignProductRepo;
    private final ShopVoucherUsageRepository shopVoucherUsageRepo;
    private final PlatformCampaignProductUsageRepository platformUsageRepo;

    @Override
    public Map<UUID, BigDecimal> computeDiscountByStore(List<StoreVoucherUse> input,
                                                        Map<UUID, List<StoreOrderItem>> storeItems) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (input == null || input.isEmpty()) return result;

        LocalDateTime now = LocalDateTime.now();

        for (StoreVoucherUse use : input) {
            UUID storeId = use.getStoreId();
            List<String> codes = use.getCodes() == null ? List.of() : use.getCodes();
            List<StoreOrderItem> items = storeItems.getOrDefault(storeId, List.of());
            if (codes.isEmpty() || items.isEmpty()) continue;

            BigDecimal applied = BigDecimal.ZERO;
            BigDecimal storeSubtotal = items.stream()
                    .map(StoreOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            for (String raw : codes) {
                String code = raw == null ? "" : raw.trim();
                if (code.isEmpty()) continue;

                ShopVoucher v = voucherRepo.findByShop_StoreIdAndCodeIgnoreCase(storeId, code).orElse(null);
                if (v == null || v.getStatus() != VoucherStatus.ACTIVE) continue;
                if (v.getStartTime()!=null && now.isBefore(v.getStartTime())) continue;
                if (v.getEndTime()!=null && now.isAfter(v.getEndTime())) continue;
                if (v.getRemainingUsage()!=null && v.getRemainingUsage()<=0) continue;

                BigDecimal eligibleSubtotal = eligibleSubtotal(v, items);
                if (v.getMinOrderValue()!=null && eligibleSubtotal.compareTo(v.getMinOrderValue())<0) continue;

                BigDecimal discount = discountOf(v, eligibleSubtotal);
                BigDecimal cap = storeSubtotal.subtract(applied);
                if (cap.signum()<=0) break;
                if (discount.compareTo(cap)>0) discount = cap;

                applied = applied.add(discount);

                if (v.getRemainingUsage()!=null && v.getRemainingUsage()>0) {
                    v.setRemainingUsage(v.getRemainingUsage()-1);
                }
            }
            if (applied.signum()>0) result.put(storeId, applied);
        }
        return result;
    }

    private BigDecimal eligibleSubtotal(ShopVoucher v, List<StoreOrderItem> items) {
        List<ShopVoucherProduct> binds = v.getVoucherProducts();
        if (binds == null || binds.isEmpty()) {
            return items.stream().map(StoreOrderItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        Set<UUID> allowed = binds.stream()
                .filter(ShopVoucherProduct::isActive)
                .map(bp -> bp.getProduct()!=null ? bp.getProduct().getProductId() : null)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        return items.stream()
                .filter(it -> "PRODUCT".equalsIgnoreCase(it.getType())
                        && it.getRefId()!=null && allowed.contains(it.getRefId()))
                .map(StoreOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal discountOf(ShopVoucher v, BigDecimal base) {
        if (base==null) base = BigDecimal.ZERO;
        if (v.getType()== VoucherType.FIXED) {
            BigDecimal d = v.getDiscountValue()==null? BigDecimal.ZERO : v.getDiscountValue();
            return d.max(BigDecimal.ZERO).min(base);
        }
        if (v.getType()== VoucherType.PERCENT) {
            int p = v.getDiscountPercent()==null? 0 : v.getDiscountPercent();
            BigDecimal raw = base.multiply(BigDecimal.valueOf(p)).divide(BigDecimal.valueOf(100));
            BigDecimal cap = v.getMaxDiscountValue()==null? raw : v.getMaxDiscountValue();
            return raw.min(cap).max(BigDecimal.ZERO);
        }
        if (v.getType()== VoucherType.SHIPPING) {
            BigDecimal d = v.getDiscountValue()==null? BigDecimal.ZERO : v.getDiscountValue();
            return d.max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlatformVoucherResult computePlatformDiscounts(
            UUID customerId,
            List<PlatformVoucherUse> platformVouchers,
            Map<UUID, List<StoreOrderItem>> storeItemsMap
    ) {
        PlatformVoucherResult out = new PlatformVoucherResult();
        if (platformVouchers == null || platformVouchers.isEmpty()) return out;

        for (PlatformVoucherUse use : platformVouchers) {
            if (use == null || use.getCampaignProductId() == null) continue;

            var cpOpt = campaignProductRepo.findUsableById(use.getCampaignProductId());
            if (cpOpt.isEmpty()) continue;
            var cp = cpOpt.get();

            // ✅ CHECK remainingUsage
            if (cp.getRemainingUsage() != null && cp.getRemainingUsage() <= 0) {
                continue;
            }

            // ✅ CHECK usagePerUser theo customer
            if (customerId != null && cp.getUsagePerUser() != null && cp.getUsagePerUser() > 0) {
                PlatformCampaignProductUsage usage = platformUsageRepo
                        .findByCampaignProduct_IdAndCustomer_Id(cp.getId(), customerId)
                        .orElse(null);

                int usedCount = usage != null ? usage.getUsedCount() : 0;
                if (usedCount >= cp.getUsagePerUser()) {
                    // user này hết lượt dùng voucher này
                    continue;
                }
            }

            // 1) gom tất cả StoreOrderItem liên quan đến product này
            Map<UUID, BigDecimal> eligibleSubtotalByStore = new HashMap<>();
            BigDecimal eligibleSubtotalTotal = BigDecimal.ZERO;

            for (Map.Entry<UUID, List<StoreOrderItem>> e : storeItemsMap.entrySet()) {
                UUID storeId = e.getKey();
                BigDecimal sum = e.getValue().stream()
                        .filter(item -> matchesCampaignProduct(item, cp))
                        .map(StoreOrderItem::getLineTotal)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sum.signum() > 0) {
                    eligibleSubtotalByStore.put(storeId, sum);
                    eligibleSubtotalTotal = eligibleSubtotalTotal.add(sum);
                }
            }

            if (eligibleSubtotalTotal.signum() <= 0) continue; // nothing to discount

            // 2) tính số tiền giảm của voucher hiện tại
            BigDecimal rawDiscount = BigDecimal.ZERO;
            switch (cp.getType()) {
                case FIXED -> {
                    BigDecimal val = Optional.ofNullable(cp.getDiscountValue()).orElse(BigDecimal.ZERO);
                    rawDiscount = val.min(eligibleSubtotalTotal);
                }
                case PERCENT -> {
                    Integer pct = Optional.ofNullable(cp.getDiscountPercent()).orElse(0);
                    BigDecimal cut = eligibleSubtotalTotal.multiply(BigDecimal.valueOf(pct))
                            .divide(BigDecimal.valueOf(100));
                    BigDecimal cap = Optional.ofNullable(cp.getMaxDiscountValue()).orElse(null);
                    if (cap != null && cap.signum() > 0) cut = cut.min(cap);
                    rawDiscount = cut;
                }
                case SHIPPING -> {
                    // Giảm vào tổng theo thiết kế “toàn sàn shipping”
                    BigDecimal val = Optional.ofNullable(cp.getDiscountValue()).orElse(BigDecimal.ZERO);
                    rawDiscount = val; // có thể cap theo shippingFeeTotal nếu muốn
                }
                default -> {}
            }
            if (rawDiscount.signum() <= 0) continue;

            // 3) phân bổ theo tỷ lệ eligibleSubtotal của từng store
            Map<UUID, BigDecimal> alloc = proportionalAllocate(rawDiscount, eligibleSubtotalByStore, eligibleSubtotalTotal);

            // 4) merge vào output
            for (Map.Entry<UUID, BigDecimal> a : alloc.entrySet()) {
                out.discountByStore.merge(a.getKey(), a.getValue(), BigDecimal::add);
            }
            String key = cp.getCampaign() != null && cp.getCampaign().getCode() != null
                    ? cp.getCampaign().getCode()
                    : String.valueOf(cp.getId());
            out.platformDiscountMap.merge(key, rawDiscount, BigDecimal::add);

            // ✅ trừ remainingUsage
            if (cp.getRemainingUsage() != null && cp.getRemainingUsage() > 0) {
                cp.setRemainingUsage(cp.getRemainingUsage() - 1);
            }

            // ✅ update usage per customer
            if (customerId != null) {
                PlatformCampaignProductUsage usage = platformUsageRepo
                        .findByCampaignProduct_IdAndCustomer_Id(cp.getId(), customerId)
                        .orElseGet(() -> {
                            Customer c = new Customer();
                            c.setId(customerId); // id từ BaseEntity

                            return PlatformCampaignProductUsage.builder()
                                    .campaignProduct(cp)
                                    .customer(c)
                                    .usedCount(0)
                                    .build();
                        });


                int newCount = usage.getUsedCount() + 1;
                usage.setUsedCount(newCount);
                LocalDateTime now = LocalDateTime.now();
                if (usage.getFirstUsedAt() == null) usage.setFirstUsedAt(now);
                usage.setLastUsedAt(now);
                platformUsageRepo.save(usage);
            }
        }

        return out;
    }

    private boolean matchesCampaignProduct(StoreOrderItem item, PlatformCampaignProduct cp) {
        // item.refId là UUID của Product/Combo — ở đây platform áp cho PRODUCT
        if (item.getRefId() == null) return false;
        return cp.getProduct() != null && cp.getProduct().getProductId().equals(item.getRefId());
    }

    private Map<UUID, BigDecimal> proportionalAllocate(BigDecimal amount,
                                                       Map<UUID, BigDecimal> baseByStore,
                                                       BigDecimal baseTotal) {
        Map<UUID, BigDecimal> r = new HashMap<>();
        if (amount.signum() <= 0 || baseByStore.isEmpty() || baseTotal.signum() <= 0) return r;

        BigDecimal allocated = BigDecimal.ZERO;
        UUID lastKey = null;
        for (Map.Entry<UUID, BigDecimal> e : baseByStore.entrySet()) {
            lastKey = e.getKey();
            BigDecimal ratio = e.getValue().divide(baseTotal, 6, java.math.RoundingMode.HALF_UP);
            BigDecimal part = amount.multiply(ratio).setScale(0, java.math.RoundingMode.DOWN);
            r.put(e.getKey(), part);
            allocated = allocated.add(part);
        }
        // dồn phần lẻ còn lại vào store cuối để tổng đúng
        BigDecimal remain = amount.subtract(allocated);
        if (remain.signum() > 0 && lastKey != null) {
            r.put(lastKey, r.get(lastKey).add(remain));
        }
        return r;
    }

    @Override
    public StoreVoucherResult computeDiscountByStoreWithDetail(UUID customerId,List<StoreVoucherUse> input,
                                                               Map<UUID, List<StoreOrderItem>> storeItems) {
        StoreVoucherResult out = new StoreVoucherResult();
        if (input == null || input.isEmpty()) return out;

        LocalDateTime now = LocalDateTime.now();

        for (StoreVoucherUse use : input) {
            UUID storeId = use.getStoreId();
            List<String> codes = use.getCodes() == null ? List.of() : use.getCodes();
            List<StoreOrderItem> items = storeItems.getOrDefault(storeId, List.of());
            if (codes.isEmpty() || items.isEmpty()) continue;

            BigDecimal storeSubtotal = items.stream()
                    .map(StoreOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> detail = out.detailByStore.computeIfAbsent(storeId, k -> new LinkedHashMap<>());

            // ===== BƯỚC 1: Áp dụng TẤT CẢ FIXED vouchers trước =====
            BigDecimal fixedDiscountTotal = BigDecimal.ZERO;
            List<ShopVoucher> validFixedVouchers = new ArrayList<>();

            for (String raw : codes) {
                String code = raw == null ? "" : raw.trim();
                if (code.isEmpty()) continue;

                ShopVoucher v = voucherRepo.findByShop_StoreIdAndCodeIgnoreCase(storeId, code).orElse(null);
                if (v == null || v.getStatus() != VoucherStatus.ACTIVE) continue;
                if (v.getType() != VoucherType.FIXED) continue; // Chỉ lấy FIXED
                if (v.getStartTime()!=null && now.isBefore(v.getStartTime())) continue;
                if (v.getEndTime()!=null && now.isAfter(v.getEndTime())) continue;
                if (v.getRemainingUsage()!=null && v.getRemainingUsage()<=0) continue;

                if (customerId != null && v.getUsagePerUser() != null && v.getUsagePerUser() > 0) {
                    ShopVoucherUsage usage = shopVoucherUsageRepo
                            .findByVoucher_IdAndCustomer_Id(v.getId(), customerId)
                            .orElse(null);

                    int usedCount = usage != null ? usage.getUsedCount() : 0;
                    if (usedCount >= v.getUsagePerUser()) {
                        continue;
                    }
                }

                BigDecimal eligibleSubtotal = eligibleSubtotal(v, items);
                if (v.getMinOrderValue()!=null && eligibleSubtotal.compareTo(v.getMinOrderValue())<0) continue;

                BigDecimal discount = discountOf(v, eligibleSubtotal);
                BigDecimal cap = storeSubtotal.subtract(fixedDiscountTotal);
                if (cap.signum()<=0) break;
                if (discount.compareTo(cap)>0) discount = cap;

                if (discount.signum() > 0) {
                    fixedDiscountTotal = fixedDiscountTotal.add(discount);
                    detail.merge(code, discount, BigDecimal::add);
                    validFixedVouchers.add(v);
                }
            }

            // ===== BƯỚC 2: Tính baseAmount sau khi trừ FIXED discount =====
            BigDecimal baseAmountForPercent = storeSubtotal.subtract(fixedDiscountTotal);
            if (baseAmountForPercent.signum() < 0) baseAmountForPercent = BigDecimal.ZERO;

            // ===== BƯỚC 3: Áp dụng PERCENT và SHIPPING vouchers =====
            BigDecimal percentAndShippingTotal = BigDecimal.ZERO;
            List<ShopVoucher> validPercentVouchers = new ArrayList<>();

            for (String raw : codes) {
                String code = raw == null ? "" : raw.trim();
                if (code.isEmpty()) continue;

                ShopVoucher v = voucherRepo.findByShop_StoreIdAndCodeIgnoreCase(storeId, code).orElse(null);
                if (v == null || v.getStatus() != VoucherStatus.ACTIVE) continue;
                if (v.getType() == VoucherType.FIXED) continue; // Bỏ qua FIXED đã xử lý
                if (v.getStartTime()!=null && now.isBefore(v.getStartTime())) continue;
                if (v.getEndTime()!=null && now.isAfter(v.getEndTime())) continue;
                if (v.getRemainingUsage()!=null && v.getRemainingUsage()<=0) continue;

                if (customerId != null && v.getUsagePerUser() != null && v.getUsagePerUser() > 0) {
                    ShopVoucherUsage usage = shopVoucherUsageRepo
                            .findByVoucher_IdAndCustomer_Id(v.getId(), customerId)
                            .orElse(null);

                    int usedCount = usage != null ? usage.getUsedCount() : 0;
                    if (usedCount >= v.getUsagePerUser()) {
                        continue;
                    }
                }

                // Đối với PERCENT: tính trên baseAmountForPercent (đã trừ FIXED)
                BigDecimal eligibleSubtotal = eligibleSubtotal(v, items);
                if (v.getMinOrderValue()!=null && eligibleSubtotal.compareTo(v.getMinOrderValue())<0) continue;

                BigDecimal discount;
                if (v.getType() == VoucherType.PERCENT) {
                    // Tính % trên baseAmountForPercent
                    int pct = v.getDiscountPercent() == null ? 0 : v.getDiscountPercent();
                    BigDecimal percentDiscount = baseAmountForPercent.multiply(BigDecimal.valueOf(pct))
                            .divide(BigDecimal.valueOf(100), java.math.RoundingMode.HALF_UP);
                    BigDecimal cap = v.getMaxDiscountValue() == null ? percentDiscount : v.getMaxDiscountValue();
                    discount = percentDiscount.min(cap).max(BigDecimal.ZERO);
                } else {
                    // SHIPPING hoặc loại khác
                    discount = discountOf(v, eligibleSubtotal);
                }

                BigDecimal cap = baseAmountForPercent.subtract(percentAndShippingTotal);
                if (cap.signum()<=0) break;
                if (discount.compareTo(cap)>0) discount = cap;

                if (discount.signum() > 0) {
                    percentAndShippingTotal = percentAndShippingTotal.add(discount);
                    detail.merge(code, discount, BigDecimal::add);
                    validPercentVouchers.add(v);
                }
            }

            // ===== BƯỚC 4: Cập nhật usage cho tất cả vouchers đã dùng =====
            List<ShopVoucher> allValidVouchers = new ArrayList<>();
            allValidVouchers.addAll(validFixedVouchers);
            allValidVouchers.addAll(validPercentVouchers);

            for (ShopVoucher v : allValidVouchers) {
                // ✅ trừ remainingUsage
                if (v.getRemainingUsage() != null && v.getRemainingUsage() > 0) {
                    v.setRemainingUsage(v.getRemainingUsage() - 1);
                }

                // ✅ update usage per customer
                if (customerId != null) {
                    ShopVoucherUsage usage = shopVoucherUsageRepo
                            .findByVoucher_IdAndCustomer_Id(v.getId(), customerId)
                            .orElseGet(() -> {
                                Customer c = new Customer();
                                c.setId(customerId);

                                return ShopVoucherUsage.builder()
                                        .voucher(v)
                                        .customer(c)
                                        .usedCount(0)
                                        .build();
                            });

                    int newCount = usage.getUsedCount() + 1;
                    usage.setUsedCount(newCount);
                    LocalDateTime nowL = LocalDateTime.now();
                    if (usage.getFirstUsedAt() == null) usage.setFirstUsedAt(nowL);
                    usage.setLastUsedAt(nowL);
                    shopVoucherUsageRepo.save(usage);
                }
            }

            // ===== BƯỚC 5: Lưu tổng discount cho store =====
            BigDecimal totalApplied = fixedDiscountTotal.add(percentAndShippingTotal);
            if (totalApplied.signum()>0) {
                out.discountByStore.merge(storeId, totalApplied, BigDecimal::add);
            }
        }
        return out;
    }

    @Override
    public BaseResponse<Map<String, Object>> getShopVoucherUsage(
            UUID storeId,
            UUID voucherId,
            UUID customerId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        List<ShopVoucherUsage> all = shopVoucherUsageRepo.findAll();

        List<ShopVoucherUsage> filtered = all.stream()
                .filter(u -> storeId == null
                        || (u.getVoucher() != null
                        && u.getVoucher().getShop() != null
                        && storeId.equals(u.getVoucher().getShop().getStoreId())))
                .filter(u -> voucherId == null
                        || (u.getVoucher() != null
                        && voucherId.equals(u.getVoucher().getId())))
                .filter(u -> customerId == null
                        || (u.getCustomer() != null
                        && customerId.equals(u.getCustomer().getId())))
                .filter(u -> {
                    if (from == null && to == null) return true;
                    LocalDateTime t = u.getLastUsedAt() != null ? u.getLastUsedAt() : u.getFirstUsedAt();
                    if (t == null) return false;
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && t.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(
                        (ShopVoucherUsage u) ->
                                u.getLastUsedAt() != null ? u.getLastUsedAt() : u.getFirstUsedAt()
                ).reversed())
                .toList();

        int total = filtered.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<ShopVoucherUsage> pageList = filtered.subList(fromIdx, toIdx);

        List<ShopVoucherUsageResponse> content = pageList.stream()
                .map(u -> {
                    ShopVoucher v = u.getVoucher();
                    Store store = v != null ? v.getShop() : null;
                    Customer c = u.getCustomer();

                    return ShopVoucherUsageResponse.builder()
                            .id(u.getId())
                            .voucherId(v != null ? v.getId() : null)
                            .voucherCode(v != null ? v.getCode() : null)
                            .voucherTitle(v != null ? v.getTitle() : null)
                            .storeId(store != null ? store.getStoreId() : null)
                            .storeName(store != null ? store.getStoreName() : null)
                            .customerId(c != null ? c.getId() : null)
                            .customerName(c != null ? c.getFullName() : null)
                            .customerEmail(c != null ? c.getEmail() : null)
                            .usedCount(u.getUsedCount())
                            .firstUsedAt(u.getFirstUsedAt())
                            .lastUsedAt(u.getLastUsedAt())
                            .build();
                })
                .toList();

        Map<String, Object> data = Map.of(
                "page", page,
                "size", size,
                "totalElements", total,
                "content", content
        );

        return BaseResponse.success("✅ Danh sách lịch sử sử dụng shop voucher", data);
    }

    @Override
    public BaseResponse<Map<String, Object>> getPlatformVoucherUsage(
            UUID campaignId,
            UUID campaignProductId,
            UUID storeId,
            UUID customerId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        List<PlatformCampaignProductUsage> all = platformUsageRepo.findAll();

        List<PlatformCampaignProductUsage> filtered = all.stream()
                .filter(u -> campaignId == null
                        || (u.getCampaignProduct() != null
                        && u.getCampaignProduct().getCampaign() != null
                        && campaignId.equals(u.getCampaignProduct().getCampaign().getId())))
                .filter(u -> campaignProductId == null
                        || (u.getCampaignProduct() != null
                        && campaignProductId.equals(u.getCampaignProduct().getId())))
                .filter(u -> storeId == null
                        || (u.getCampaignProduct() != null
                        && u.getCampaignProduct().getStore() != null
                        && storeId.equals(u.getCampaignProduct().getStore().getStoreId())))
                .filter(u -> customerId == null
                        || (u.getCustomer() != null
                        && customerId.equals(u.getCustomer().getId())))
                .filter(u -> {
                    if (from == null && to == null) return true;
                    LocalDateTime t = u.getLastUsedAt() != null ? u.getLastUsedAt() : u.getFirstUsedAt();
                    if (t == null) return false;
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && t.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(
                        (PlatformCampaignProductUsage u) ->
                                u.getLastUsedAt() != null ? u.getLastUsedAt() : u.getFirstUsedAt()
                ).reversed())
                .toList();

        int total = filtered.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<PlatformCampaignProductUsage> pageList = filtered.subList(fromIdx, toIdx);

        List<PlatformVoucherUsageResponse> content = pageList.stream()
                .map(u -> {
                    PlatformCampaignProduct cp = u.getCampaignProduct();
                    PlatformCampaign cpn = cp != null ? cp.getCampaign() : null;
                    Product prod = cp != null ? cp.getProduct() : null;
                    Store store = cp != null ? cp.getStore() : null;
                    Customer cus = u.getCustomer();

                    return PlatformVoucherUsageResponse.builder()
                            .id(u.getId())
                            .campaignId(cpn != null ? cpn.getId() : null)
                            .campaignCode(cpn != null ? cpn.getCode() : null)
                            .campaignName(cpn != null ? cpn.getName() : null)
                            .campaignType(cpn != null && cpn.getCampaignType() != null
                                    ? cpn.getCampaignType().name()
                                    : null)
                            .campaignProductId(cp != null ? cp.getId() : null)
                            .productId(prod != null ? prod.getProductId() : null)
                            .productName(prod != null ? prod.getName() : null)
                            .storeId(store != null ? store.getStoreId() : null)
                            .storeName(store != null ? store.getStoreName() : null)
                            .customerId(cus != null ? cus.getId() : null)
                            .customerName(cus != null ? cus.getFullName() : null)
                            .customerEmail(cus != null ? cus.getEmail() : null)
                            .usedCount(u.getUsedCount())
                            .firstUsedAt(u.getFirstUsedAt())
                            .lastUsedAt(u.getLastUsedAt())
                            .build();
                })
                .toList();

        Map<String, Object> data = Map.of(
                "page", page,
                "size", size,
                "totalElements", total,
                "content", content
        );

        return BaseResponse.success("✅ Danh sách lịch sử sử dụng platform voucher", data);
    }

}
