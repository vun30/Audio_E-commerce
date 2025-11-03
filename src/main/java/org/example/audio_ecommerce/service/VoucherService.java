package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.PlatformVoucherUse;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.entity.StoreOrderItem;

import java.math.BigDecimal;
import java.util.*;

public interface VoucherService {
    Map<UUID, BigDecimal> computeDiscountByStore(
            List<StoreVoucherUse> vouchersInput,
            Map<UUID, List<StoreOrderItem>> storeItems
    );

    // a) Tổng giảm theo từng store (để phân bổ xuống StoreOrder)
    // b) Mapping <voucherCodeOrId, amount> cho toàn đơn (để trả response + lưu JSON)
    PlatformVoucherResult computePlatformDiscounts(List<PlatformVoucherUse> platformVouchers,
                                                   Map<UUID, List<StoreOrderItem>> storeItemsMap);

    class PlatformVoucherResult {
        public Map<UUID, BigDecimal> discountByStore = new HashMap<>();
        public Map<String, BigDecimal> platformDiscountMap = new LinkedHashMap<>(); // code/id -> amount
        public String toPlatformVoucherJson() {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(platformDiscountMap);
            } catch (Exception e) { return "{}"; }
        }
        public Map<UUID, String> toPerStoreJson() {
            // Chỉ dùng khi bạn muốn lưu chi tiết theo store — ở đây mình lưu 1 map đơn giản cho platformJson ở từng store
            Map<UUID, String> json = new HashMap<>();
            try {
                String all = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(platformDiscountMap);
                storeItemsMapKeySet(discountByStore).forEach(storeId -> json.put(storeId, all));
            } catch (Exception e) { /* ignore */ }
            return json;
        }
        private Set<UUID> storeItemsMapKeySet(Map<UUID, BigDecimal> m) { return m.keySet(); }
    }
}
