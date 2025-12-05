package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.repository.ReturnRequestRepository;
import org.example.audio_ecommerce.repository.StoreOrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutEligibilityService {

    private final StoreOrderItemRepository itemRepo;
    private final ReturnRequestRepository returnRepo;

    // ========================================================
    // CHECK 7-DAY + RETURN STATUS → CHO PHÉP PAYOUT
    // ========================================================
    @Transactional
    public void evaluateEligibility() {

        List<StoreOrderItem> items =
            itemRepo.findByEligibleForPayoutFalseAndIsPayoutFalse();

        for (StoreOrderItem item : items) {

            // 1. Chưa giao hàng
            if (item.getDeliveredAt() == null) {
                log.info("[SKIP] Item {} chưa giao hàng", item.getId());
                continue;
            }

            // 2. Chưa đủ 7 ngày
            LocalDateTime delivered = item.getDeliveredAt();
            if (delivered.plusDays(7).isAfter(LocalDateTime.now())) {
                log.info("[WAIT] Item {} chưa đủ 7 ngày (giao: {})", item.getId(), delivered);
                continue;
            }

            // 3. Check ReturnRequest
            Optional<ReturnRequest> rrOpt =
                    returnRepo.findTopByOrderItemIdOrderByCreatedAtDesc(item.getId());

            if (rrOpt.isPresent()) {

                ReturnRequest rr = rrOpt.get();
                ReturnStatus status = rr.getStatus();

                // BLOCK trạng thái return không cho payout
                if (status != ReturnStatus.CANCELED &&
                    status != ReturnStatus.DISPUTE_RESOLVED_SHOP) {

                    log.info("[BLOCK] Item {} bị block bởi ReturnStatus: {}",
                              item.getId(), status);
                    continue;
                }

            }


            // 4. UNLOCK cho payout
            item.setEligibleForPayout(true);
            itemRepo.save(item);

            log.info("[ELIGIBLE] Item {} đã mở khóa payout", item.getId());
        }
    }


    // ========================================================
    // ĐÁNH DẤU ITEM ĐÃ RETURN SAU KHI REFUNDED
    // ========================================================
    @Transactional
    public void checkReturnedItems() {

        List<ReturnRequest> requests =
                returnRepo.findAllByStatus(ReturnStatus.REFUNDED);

        for (ReturnRequest rr : requests) {

            UUID itemId = rr.getOrderItemId();
            Optional<StoreOrderItem> itemOpt = itemRepo.findById(itemId);

            if (itemOpt.isEmpty()) {
                log.warn("[WARN] Không tìm thấy StoreOrderItem ID {}", itemId);
                continue;
            }

            StoreOrderItem item = itemOpt.get();

            if (!item.getIsReturned()) {
                item.setIsReturned(true);
                itemRepo.save(item);

                log.info("[RETURNED] Item {} đã set isReturned = true", item.getId());
            }
        }
    }


    // ========================================================
    // SYNC deliveredAt TỪ STORE_ORDER → ITEM
    // ========================================================
    @Transactional
    public void syncDeliveredAtForItems() {

        List<StoreOrderItem> items =
            itemRepo.findAllByDeliveredAtIsNullAndStoreOrder_DeliveredAtIsNotNull();

        if (items.isEmpty()) {
            log.info("🔍 Không có item nào cần sync deliveredAt.");
            return;
        }

        log.info("🚚 Sync deliveredAt cho {} items", items.size());

        for (StoreOrderItem item : items) {

            LocalDateTime deliveredAt = item.getStoreOrder().getDeliveredAt();
            if (deliveredAt == null) continue;

            item.setDeliveredAt(deliveredAt);
            itemRepo.save(item);

            log.info("✅ deliveredAt={} cập nhật cho item {}", deliveredAt, item.getId());
        }

        log.info("🎉 Hoàn tất sync deliveredAt.");
    }

  // ========================================================
// 🧮 TÍNH PHÍ SHIP CHÊNH LỆCH (GHN thực tế - phí dự kiến)
// ========================================================
@Transactional
public void calculateShippingFeeDifference() {

    // Lấy tất cả items có storeOrder chứa phí ship thực tế
    List<StoreOrderItem> items =
            itemRepo.findAllByStoreOrder_ShippingFeeRealIsNotNull();

    if (items.isEmpty()) {
        log.info("⛔ Không có item nào thuộc đơn có phí ship thực tế để tính chênh lệch.");
        return;
    }

    log.info("🚚 Bắt đầu tính chênh lệch phí ship cho {} items", items.size());

    for (StoreOrderItem item : items) {

        var order = item.getStoreOrder();

        BigDecimal estimated =
                order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();

        BigDecimal actual =
                order.getShippingFeeReal() == null ? BigDecimal.ZERO : order.getShippingFeeReal();

        // CHÊNH = thực tế - dự kiến
        BigDecimal diff = actual.subtract(estimated);

        // =====================================================
        // Nếu diff <= 0 → shop KHÔNG phải trả phí (set = 0)
        // =====================================================
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {

            order.setShippingFeeForStore(BigDecimal.ZERO);

            itemRepo.save(item);

            log.info("⚠️ Order {} | FeeReal={} <= FeeExpected={} → Không tính phí shop (set 0)",
                    order.getId(), actual, estimated);

            continue;
        }

        // =====================================================
        // diff > 0 → shop phải trả
        // =====================================================
        order.setShippingFeeForStore(diff);

        itemRepo.save(item);

        log.info("📦 Order {} | Expected={} | Real={} | Diff={} (Shop trả)",
                order.getId(), estimated, actual, diff);
    }

    log.info("🎉 Hoàn tất tính phí ship chênh lệch.");
}

}
