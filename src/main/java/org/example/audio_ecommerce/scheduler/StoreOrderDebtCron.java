package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.service.Impl.StoreWalletDebtService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOrderDebtCron {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletDebtService storeWalletDebtService;

    @Scheduled(fixedDelay = 30000) // mỗi 30s
    @Transactional
    public void recalcTotalDebtOrder() {

        List<StoreOrder> orders = storeOrderRepository.findOrdersForDebtCron();
        if (orders.isEmpty()) return;

        int updated = 0;

        for (StoreOrder o : orders) {

            // 1) ĐÃ THANH TOÁN NỢ -> BỎ QUA
            if (Boolean.TRUE.equals(o.getPaidByShop())) continue;

            OrderStatus status = o.getStatus();
            if (status == null) continue;

            // 2) BỎ QUA 3 TRẠNG THÁI
            if (status == OrderStatus.UNPAID
                    || status == OrderStatus.PENDING
                    || status == OrderStatus.CONFIRMED) {
                continue;
            }

            BigDecimal R = nvl(o.getShippingFeeReal());
            if (R.compareTo(BigDecimal.ZERO) <= 0) continue; // không có shipReal thì không tính

            BigDecimal E = nvl(o.getShippingFee());

            BigDecimal debt;

            // 3) ƯU TIÊN: có return charge -> 1.5R
            if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
                debt = R.multiply(new BigDecimal("1.5"))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                // 4) Không return charge
                if (o.getDeliveredAt() != null) {
                    // delivered -> shipReal - shipEstimated
                    debt = R.subtract(E).max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    // chưa delivered -> nợ = shipReal
                    debt = R.setScale(2, RoundingMode.HALF_UP);
                }
            }

            // 5) Chỉ update DB nếu totalDebtOrder thay đổi
            if (o.getTotalDebtOrder() == null || o.getTotalDebtOrder().compareTo(debt) != 0) {
                o.setTotalDebtOrder(debt);
                storeOrderRepository.save(o);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("StoreOrderDebtCron updated {} orders", updated);
        }
    }


//    @Transactional
//    public void recalcDebtForOrder(UUID storeOrderId, OrderStatus status) {
//
//        StoreOrder o = storeOrderRepository.findById(storeOrderId)
//                .orElseThrow(() -> new RuntimeException("❌ StoreOrder not found: " + storeOrderId));
//
//        BigDecimal newDebt = computeDebt(o, status);
//
//        // Nếu computeDebt trả null => nghĩa là R<=0 => không update gì
//        if (newDebt == null) return;
//
//        boolean changed = (o.getTotalDebtOrder() == null || o.getTotalDebtOrder().compareTo(newDebt) != 0);
//
//        if (changed) {
//            o.setTotalDebtOrder(newDebt);
//            storeOrderRepository.save(o);
//            log.info("recalcDebtForOrder updated order={} debt={}", storeOrderId, newDebt);
//        }
//    }

    /**
     * TÍNH NỢ - giữ đúng logic cron.
     * Trả về:
     *  - null nếu không cần update (R<=0)
     *  - debt nếu có thể tính
     */
    private BigDecimal computeDebt(StoreOrder o, OrderStatus status) {

        // ✅ ĐÃ THANH TOÁN NỢ -> BỎ QUA (không đụng vào nữa)
        if (Boolean.TRUE.equals(o.getPaidByShop())) return null;

        if (status == null) return null;

        BigDecimal R = nvl(o.getShippingFeeReal());
        BigDecimal E = nvl(o.getShippingFee());

        // ✅ CANCELLED -> XÓA NỢ
        if (status == OrderStatus.CANCELLED) {
            return BigDecimal.ZERO;
        }

        // ✅ nếu shipReal chưa có / <=0 thì không tính (trừ CANCELLED đã xử lý ở trên)
        if (R.compareTo(BigDecimal.ZERO) <= 0) return null;

        // ✅ Nếu áp dụng phí quay đầu / return charge -> nợ = 1.5R
        if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
            return R.multiply(new BigDecimal("1.5"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        switch (status) {
            case SHIPPING:
            case OUT_FOR_DELIVERY:
            case DELIVERED_WAITING_CONFIRM:
                return R;

            case DELIVERY_SUCCESS:
                return R.subtract(E).max(BigDecimal.ZERO);

            default:
                return null;
        }
    }

    @Transactional
    public void recalcDebtAndWalletForOrder(UUID storeOrderId, OrderStatus status) {

        StoreOrder o = storeOrderRepository.findById(storeOrderId)
                .orElseThrow(() -> new RuntimeException("StoreOrder not found"));

        BigDecimal newDebt = computeDebt(o, status);
        if (newDebt == null) return;

        boolean changed =
                o.getTotalDebtOrder() == null
                        || o.getTotalDebtOrder().compareTo(newDebt) != 0;

        if (!changed) return;

        o.setTotalDebtOrder(newDebt);
        storeOrderRepository.save(o);

        UUID storeId = o.getStore().getStoreId();
        storeWalletDebtService.recalcStoreDebtBalanceByStoreId(storeId);
    }



    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
