package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.PlatformVoucherUse;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.StoreOrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface VoucherService {
    Map<UUID, BigDecimal> computeDiscountByStore(
            List<StoreVoucherUse> vouchersInput,
            Map<UUID, List<StoreOrderItem>> storeItems
    );

    StoreVoucherResult computeDiscountByStoreWithDetail(
            UUID customerId,
            List<StoreVoucherUse> input,
            Map<UUID, List<StoreOrderItem>> storeItems,
            Map<UUID, BigDecimal> platformDiscountByStore
    );

    // a) Tổng giảm theo từng store (để phân bổ xuống StoreOrder)
    // b) Mapping <voucherCodeOrId, amount> cho toàn đơn (để trả response + lưu JSON)
    PlatformVoucherResult computePlatformDiscounts(UUID customerId, List<PlatformVoucherUse> platformVouchers,
                                                   Map<UUID, List<StoreOrderItem>> storeItemsMap);

    class PlatformVoucherResult {
        public Map<UUID, BigDecimal> discountByStore = new HashMap<>();
        public Map<String, BigDecimal> platformDiscountMap = new LinkedHashMap<>(); // code/id -> amount

        public String toPlatformVoucherJson() {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(platformDiscountMap);
            } catch (Exception e) {
                return "{}";
            }
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

        private Set<UUID> storeItemsMapKeySet(Map<UUID, BigDecimal> m) {
            return m.keySet();
        }
    }

    class StoreVoucherResult {
        /**
         * Tổng giảm theo store
         */
        public Map<UUID, BigDecimal> discountByStore = new HashMap<>();
        /**
         * Chi tiết theo store: { storeId -> { CODE -> amount } }
         */
        public Map<UUID, Map<String, BigDecimal>> detailByStore = new HashMap<>();

        /**
         * Xuất JSON cho từng store từ detailByStore
         */
        public Map<UUID, String> toDetailJsonByStore() {
            Map<UUID, String> json = new HashMap<>();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            detailByStore.forEach((sid, map) -> {
                try {
                    json.put(sid, om.writeValueAsString(map));
                } catch (Exception e) {
                    json.put(sid, "{}");
                }
            });
            return json;
        }
    }

    BaseResponse<Map<String, Object>> getShopVoucherUsage(
            UUID storeId,
            UUID voucherId,
            UUID customerId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    BaseResponse<Map<String, Object>> getPlatformVoucherUsage(
            UUID campaignId,
            UUID campaignProductId,
            UUID storeId,
            UUID customerId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );
}
