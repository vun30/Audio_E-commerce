// service/Impl/VoucherServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.PlatformVoucherUse;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherRepository;
import org.example.audio_ecommerce.service.VoucherService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final ShopVoucherRepository voucherRepo;
    private final PlatformCampaignProductRepository campaignProductRepo;

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
    public StoreVoucherResult computeDiscountByStoreWithDetail(List<StoreVoucherUse> input,
                                                               Map<UUID, List<StoreOrderItem>> storeItems) {
        StoreVoucherResult out = new StoreVoucherResult();
        if (input == null || input.isEmpty()) return out;

        LocalDateTime now = LocalDateTime.now();

        for (StoreVoucherUse use : input) {
            UUID storeId = use.getStoreId();
            List<String> codes = use.getCodes() == null ? List.of() : use.getCodes();
            List<StoreOrderItem> items = storeItems.getOrDefault(storeId, List.of());
            if (codes.isEmpty() || items.isEmpty()) continue;

            BigDecimal applied = BigDecimal.ZERO;
            BigDecimal storeSubtotal = items.stream()
                    .map(StoreOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> detail = out.detailByStore.computeIfAbsent(storeId, k -> new LinkedHashMap<>());

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

                if (discount.signum() > 0) {
                    applied = applied.add(discount);
                    // ✅ lưu chi tiết theo mã
                    detail.merge(code, discount, BigDecimal::add);
                    if (v.getRemainingUsage()!=null && v.getRemainingUsage()>0) {
                        v.setRemainingUsage(v.getRemainingUsage()-1);
                    }
                }
            }
            if (applied.signum()>0) {
                out.discountByStore.merge(storeId, applied, BigDecimal::add);
            }
        }
        return out;
    }

}
