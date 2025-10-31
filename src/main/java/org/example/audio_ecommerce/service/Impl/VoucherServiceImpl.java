// service/Impl/VoucherServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
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
}
