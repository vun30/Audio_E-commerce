// service/Impl/VoucherServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
            // Use original price snapshot (linePriceBeforeDiscount) as the base
            BigDecimal storeSubtotal = items.stream()
                    .map(it -> Optional.ofNullable(it.getLinePriceBeforeDiscount())
                            .orElse(it.getLineTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        // Calculate on original (before-discount) line price when available
        return items.stream()
                .map(it -> Optional.ofNullable(it.getLinePriceBeforeDiscount())
                        .orElse(it.getLineTotal()))
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

    private int eligibleQuantity(ShopVoucher v, List<StoreOrderItem> items) {
        List<ShopVoucherProduct> binds = v.getVoucherProducts();
        if (binds == null || binds.isEmpty()) {
            // áp cho toàn bộ sản phẩm của shop
            return items.stream()
                    .mapToInt(it -> Optional.ofNullable(it.getQuantity()).orElse(0))
                    .sum();
        }
        Set<UUID> allowed = binds.stream()
                .filter(ShopVoucherProduct::isActive)
                .map(bp -> bp.getProduct() != null ? bp.getProduct().getProductId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(it -> "PRODUCT".equalsIgnoreCase(it.getType())
                        && it.getRefId() != null
                        && allowed.contains(it.getRefId()))
                .mapToInt(it -> Optional.ofNullable(it.getQuantity()).orElse(0))
                .sum();
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

        //
        log.info("[PLATFORM_VOUCHER] customerId={}, platformVouchers={}, storeCount={}",
                customerId,
                platformVouchers.size(),
                storeItemsMap != null ? storeItemsMap.size() : 0
        );
        //
        for (PlatformVoucherUse use : platformVouchers) {
            if (use == null || use.getCampaignProductId() == null) continue;

            //
            log.info("[PLATFORM_VOUCHER] >>> Handling campaignProductId={}", use.getCampaignProductId());
            //
            var cpOpt = campaignProductRepo.findUsableById(use.getCampaignProductId());
            if (cpOpt.isEmpty()){
                campaignProductRepo.findById(use.getCampaignProductId()).ifPresentOrElse(
                        cpRaw -> log.info(
                                "[PLATFORM_VOUCHER][DEBUG_RAW] id={}, status={}, remainingUsage={}, usagePerUser={}, " +
                                        "startTime={}, endTime={}",
                                cpRaw.getId(),
                                cpRaw.getStatus(),
                                cpRaw.getRemainingUsage(),
                                cpRaw.getUsagePerUser(),
                                cpRaw.getStartTime(),
                                cpRaw.getEndTime()
                        ),
                        () -> log.warn("[PLATFORM_VOUCHER][DEBUG_RAW] id={} NOT FOUND IN DB",
                                use.getCampaignProductId())
                );
                log.info("[PLATFORM_VOUCHER] campaignProductId={} NOT USABLE -> skip", use.getCampaignProductId());
                continue;}
            var cp = cpOpt.get();

            log.info("[PLATFORM_VOUCHER] cpId={}, productId={}, type={}, remainingUsage={}, usagePerUser={}",
                    cp.getId(),
                    cp.getProduct() != null ? cp.getProduct().getProductId() : null,
                    cp.getType(),
                    cp.getRemainingUsage(),
                    cp.getUsagePerUser()
            );

            // ✅ CHECK remainingUsage (tổng toàn hệ thống)
            if (cp.getRemainingUsage() != null && cp.getRemainingUsage() <= 0) {
                log.info("[PLATFORM_VOUCHER] cpId={} remainingUsage={} <= 0 -> SKIP",
                        cp.getId(), cp.getRemainingUsage());
                continue;
            }

            // 1) Gom subtotal & quantity theo store cho campaign product này
            Map<UUID, BigDecimal> eligibleSubtotalByStore = new HashMap<>();
            BigDecimal eligibleSubtotalTotal = BigDecimal.ZERO;
            int totalEligibleQty = 0;

            for (Map.Entry<UUID, List<StoreOrderItem>> e : storeItemsMap.entrySet()) {
                UUID storeId = e.getKey();
                BigDecimal sum = BigDecimal.ZERO;
                int localMatchCount = 0;

                for (StoreOrderItem item : e.getValue()) {
                    log.info("[PLATFORM_VOUCHER][DEBUG_ITEMS] storeId={}, itemRefId={}, itemType={}, lineTotal={}, qty={}, cpProductId={}",
                            storeId,
                            item.getRefId(),
                            item.getType(),
                            item.getLineTotal(),
                            item.getQuantity(),
                            cp.getProduct() != null ? cp.getProduct().getProductId() : null
                    );
                    if (!matchesCampaignProduct(item, cp)) continue;
                    localMatchCount++;
                    BigDecimal baseLine;
                    if (item.getLinePriceBeforeDiscount() != null) {
                        baseLine = item.getLinePriceBeforeDiscount(); // GIÁ GỐC x quantity
                    } else if (item.getUnitPriceBeforeDiscount() != null) {
                        baseLine = item.getUnitPriceBeforeDiscount()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    } else {
                        // fallback, tránh null
                        baseLine = Optional.ofNullable(item.getLineTotal()).orElse(BigDecimal.ZERO);
                    }

                    if (baseLine.signum() <= 0) continue;

                    sum = sum.add(baseLine);

                    Integer q = item.getQuantity();
                    if (q != null && q > 0) {
                        totalEligibleQty += q;
                    }
                }
                log.info("[PLATFORM_VOUCHER][STORE_SUMMARY] cpId={}, storeId={}, sum={}, localMatchCount={}",
                        cp.getId(), storeId, sum, localMatchCount);
                if (sum.signum() > 0) {
                    eligibleSubtotalByStore.put(storeId, sum);
                    eligibleSubtotalTotal = eligibleSubtotalTotal.add(sum);
                }
                log.info("[PLATFORM_VOUCHER] cpId={} eligibleSubtotalTotal={}, totalEligibleQty={}, storeMatchCount={}",
                        cp.getId(), eligibleSubtotalTotal, totalEligibleQty, eligibleSubtotalByStore.size());
            }

            if (eligibleSubtotalTotal.signum() <= 0 || totalEligibleQty <= 0) {
                // không có item nào match campaign này
                continue;
            }

            // 2) CHECK usagePerUser theo customer + quantity
            Integer usagePerUser = cp.getUsagePerUser();
            PlatformCampaignProductUsage usage = null;
            int usedCount = 0;
            log.info("[PLATFORM_VOUCHER] cpId={} customerId={} usedCount={} / usagePerUser={}",
                    cp.getId(), customerId, usedCount, usagePerUser);
            if (customerId != null && usagePerUser != null && usagePerUser > 0) {
                usage = platformUsageRepo
                        .findByCampaignProduct_IdAndCustomer_Id(cp.getId(), customerId)
                        .orElse(null);

                usedCount = (usage != null && usage.getUsedCount() != null)
                        ? usage.getUsedCount()
                        : 0;

                if (usedCount >= usagePerUser) {
                    log.info("[PLATFORM_VOUCHER] cpId={} customerId={} EXCEED usagePerUser -> SKIP",
                            cp.getId(), customerId);
                    // Hết sạch lượt
                    continue;
                }

                int remaining = usagePerUser - usedCount;
                // Nếu quantity của lần này > remaining → theo rule: không cho hưởng ưu đãi luôn
                // (giống logic trong CartServiceImpl.resolveUnitPriceInternal)
                if (totalEligibleQty > remaining) {
                    log.info("[PLATFORM_VOUCHER] cpId={} totalEligibleQty={} > remainingPerUser={} -> SKIP",
                            cp.getId(), totalEligibleQty, remaining);
                    continue;
                }
            }

            // 3) Tính số tiền giảm
            BigDecimal rawDiscount = BigDecimal.ZERO;
            switch (cp.getType()) {
                case FIXED -> {
                    BigDecimal val = Optional.ofNullable(cp.getDiscountValue()).orElse(BigDecimal.ZERO);
                    rawDiscount = val.min(eligibleSubtotalTotal);
                }
                case PERCENT -> {
                    Integer pct = Optional.ofNullable(cp.getDiscountPercent()).orElse(0);
                    BigDecimal cut = eligibleSubtotalTotal
                            .multiply(BigDecimal.valueOf(pct))
                            .divide(BigDecimal.valueOf(100));
                    BigDecimal cap = Optional.ofNullable(cp.getMaxDiscountValue()).orElse(null);
                    if (cap != null && cap.signum() > 0) cut = cut.min(cap);
                    rawDiscount = cut;
                }
                case SHIPPING -> {
                    BigDecimal val = Optional.ofNullable(cp.getDiscountValue()).orElse(BigDecimal.ZERO);
                    rawDiscount = val;
                }
                default -> {}
            }
            log.info("[PLATFORM_VOUCHER] cpId={} rawDiscount={}", cp.getId(), rawDiscount);
            if (rawDiscount.signum() <= 0) {
                log.info("[PLATFORM_VOUCHER] cpId={} rawDiscount <= 0 -> SKIP", cp.getId());
                continue;
            }

            // 4) Phân bổ theo store như cũ
            Map<UUID, BigDecimal> alloc = proportionalAllocate(
                    rawDiscount,
                    eligibleSubtotalByStore,
                    eligibleSubtotalTotal
            );
            log.info("[PLATFORM_VOUCHER] cpId={} allocByStore={}", cp.getId(), alloc);
            for (Map.Entry<UUID, BigDecimal> a : alloc.entrySet()) {
                out.discountByStore.merge(a.getKey(), a.getValue(), BigDecimal::add);
            }

            String key = cp.getCampaign() != null && cp.getCampaign().getCode() != null
                    ? cp.getCampaign().getCode()
                    : String.valueOf(cp.getId());
            out.platformDiscountMap.merge(key, rawDiscount, BigDecimal::add);

            // 5) Trừ remainingUsage (nếu bạn muốn cũng theo quantity)
            int consumeQty = totalEligibleQty; // số lượt bị "đốt" trong lần này

            if (cp.getRemainingUsage() != null && cp.getRemainingUsage() > 0) {
                int newRemain = cp.getRemainingUsage() - consumeQty;
                log.info("[PLATFORM_VOUCHER] cpId={} remainingUsage {} -> {} (consumeQty={})",
                        cp.getId(), cp.getRemainingUsage(), Math.max(newRemain, 0), consumeQty);
                cp.setRemainingUsage(Math.max(newRemain, 0));
            }

            // 6) Update usage per customer theo quantity
            if (customerId != null && usagePerUser != null && usagePerUser > 0) {
                if (usage == null) {
                    Customer c = new Customer();
                    c.setId(customerId);

                    usage = PlatformCampaignProductUsage.builder()
                            .campaignProduct(cp)
                            .customer(c)
                            .usedCount(0)
                            .build();
                }

                int newCount = usedCount + consumeQty;
                log.info("[PLATFORM_VOUCHER] cpId={} customerId={} usedCount {} -> {} (consumeQty={})",
                        cp.getId(), customerId, usedCount, newCount, consumeQty);
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
    public StoreVoucherResult computeDiscountByStoreWithDetail(
            UUID customerId,
            List<StoreVoucherUse> input,
            Map<UUID, List<StoreOrderItem>> storeItems,
            Map<UUID, BigDecimal> platformDiscountByStore
    ) {
        if (platformDiscountByStore == null) {
            platformDiscountByStore = Collections.emptyMap();
        }

        StoreVoucherResult out = new StoreVoucherResult();
        if (input == null || input.isEmpty()) return out;

        LocalDateTime now = LocalDateTime.now();

        for (StoreVoucherUse use : input) {
            UUID storeId = use.getStoreId();
            List<String> codes = use.getCodes() == null ? List.of() : use.getCodes();
            List<StoreOrderItem> items = storeItems.getOrDefault(storeId, List.of());
            if (codes.isEmpty() || items.isEmpty()) continue;

            BigDecimal applied = BigDecimal.ZERO;
        // Base subtotal for shop voucher should also use original price snapshot
        BigDecimal storeSubtotal = items.stream()
                .map(it -> Optional.ofNullable(it.getLinePriceBeforeDiscount())
                        .orElse(it.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // tổng giảm nền tảng của store này
            BigDecimal platformDiscountForStore = platformDiscountByStore
                    .getOrDefault(storeId, BigDecimal.ZERO);

            Map<String, BigDecimal> detail = out.detailByStore
                    .computeIfAbsent(storeId, k -> new LinkedHashMap<>());

            for (String raw : codes) {
                String code = raw == null ? "" : raw.trim();
                if (code.isEmpty()) continue;

                ShopVoucher v = voucherRepo.findByShop_StoreIdAndCodeIgnoreCase(storeId, code).orElse(null);
                if (v == null || v.getStatus() != VoucherStatus.ACTIVE) continue;
                if (v.getStartTime() != null && now.isBefore(v.getStartTime())) continue;
                if (v.getEndTime() != null && now.isAfter(v.getEndTime())) continue;
                if (v.getRemainingUsage() != null && v.getRemainingUsage() <= 0) continue;

                // ====== Tính subtotal & quantity đủ điều kiện ======
                BigDecimal eligibleSubtotal = eligibleSubtotal(v, items);
                int eligibleQty = eligibleQuantity(v, items);

                if (eligibleQty <= 0) continue; // không có sản phẩm nào áp voucher
                if (v.getMinOrderValue() != null && eligibleSubtotal.compareTo(v.getMinOrderValue()) < 0) continue;

                // ====== CHECK usagePerUser theo customer + quantity ======
                Integer usagePerUser = v.getUsagePerUser();
                ShopVoucherUsage usage = null;
                int usedCount = 0;

                if (customerId != null && usagePerUser != null && usagePerUser > 0) {
                    usage = shopVoucherUsageRepo
                            .findByVoucher_IdAndCustomer_Id(v.getId(), customerId)
                            .orElse(null);

                    usedCount = (usage != null && usage.getUsedCount() != null)
                            ? usage.getUsedCount()
                            : 0;

                    if (usedCount >= usagePerUser) {
                        // user này đã hết sạch lượt
                        continue;
                    }

                    int remaining = usagePerUser - usedCount;
                    // Nếu số lượng sản phẩm lần này > số lượt còn lại → block luôn voucher
                    // (giống rule bạn đang dùng cho campaign product)
                    if (eligibleQty > remaining) {
                        continue;
                    }
                }

                // ====== Base để tính giảm shop ======
                BigDecimal baseForShop;
                if (v.getType() == VoucherType.SHIPPING) {
                    // Ship voucher: cứ tính trên phần đủ điều kiện
                    baseForShop = eligibleSubtotal;
                } else {
                    // 1) Base sau platform theo toàn shop
                    BigDecimal baseAfterPlatform = storeSubtotal.subtract(platformDiscountForStore);
                    if (baseAfterPlatform.signum() <= 0) {
                        continue; // không còn gì để giảm
                    }

                    // 2) Shop voucher chỉ được áp lên phần đủ điều kiện của nó
                    // => lấy min(eligibleSubtotal, baseAfterPlatform)
                    baseForShop = eligibleSubtotal.min(baseAfterPlatform);
                }

                if (baseForShop.signum() <= 0) continue;

                BigDecimal discount = discountOf(v, baseForShop);
                BigDecimal cap = storeSubtotal.subtract(applied); // tránh giảm quá subtotal
                if (cap.signum() <= 0) break;
                if (discount.compareTo(cap) > 0) discount = cap;

                if (discount.signum() > 0) {
                    applied = applied.add(discount);
                    // ✅ lưu chi tiết theo mã
                    detail.merge(code, discount, BigDecimal::add);

                    // ====== SỐ LƯỢT BỊ TIÊU HAO THEO QUANTITY ======
                    int consumeQty = eligibleQty;

                    // ✅ trừ remainingUsage theo quantity
                    if (v.getRemainingUsage() != null && v.getRemainingUsage() > 0) {
                        int newRemain = v.getRemainingUsage() - consumeQty;
                        v.setRemainingUsage(Math.max(newRemain, 0));
                    }

                    // ✅ update usage per customer theo quantity
                    if (customerId != null && usagePerUser != null && usagePerUser > 0) {
                        if (usage == null) {
                            Customer c = new Customer();
                            c.setId(customerId);

                            usage = ShopVoucherUsage.builder()
                                    .voucher(v)
                                    .customer(c)
                                    .usedCount(0)
                                    .build();
                        }

                        int newCount = usedCount + consumeQty;
                        usage.setUsedCount(newCount);
                        LocalDateTime nowL = LocalDateTime.now();
                        if (usage.getFirstUsedAt() == null) usage.setFirstUsedAt(nowL);
                        usage.setLastUsedAt(nowL);
                        shopVoucherUsageRepo.save(usage);
                    }
                }
            }

            if (applied.signum() > 0) {
                out.discountByStore.merge(storeId, applied, BigDecimal::add);
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
