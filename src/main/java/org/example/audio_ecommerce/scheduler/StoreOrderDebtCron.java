package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
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

    @Scheduled(cron = "0 */1 * * * *") // mỗi 1 phút
    @Transactional
    public void recalcTotalDebtOrder() {

        List<StoreOrder> orders = storeOrderRepository.findOrdersForDebtCron();
        if (orders.isEmpty()) return;

        int updated = 0;

        for (StoreOrder o : orders) {

            // ✅ ĐÃ THANH TOÁN NỢ -> BỎ QUA
            if (Boolean.TRUE.equals(o.getPaidByShop())) continue;

            // ✅ Chỉ tính khi có status
            OrderStatus status = o.getStatus();
            if (status == null) continue;

            BigDecimal R = nvl(o.getShippingFeeReal());
            if (R.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal E = nvl(o.getShippingFee());

            BigDecimal debt;

            // ✅ Tính theo rule status
            switch (status) {
                case SHIPPING:
                case OUT_FOR_DELIVERY:
                case DELIVERED_WAITING_CONFIRM:
                    debt = R;
                    break;

                case RETURNING:
                    debt = R.multiply(new BigDecimal("1.5"))
                            .setScale(2, RoundingMode.HALF_UP);
                    break;

                case DELIVERY_SUCCESS:
                    debt = R.subtract(E).max(BigDecimal.ZERO);
                    break;

                default:
                    continue; // trạng thái khác -> không tính lại nợ
            }

            // ✅ Giữ logic cũ: nếu returnChargeApplied true => cộng thêm rate% của R
            if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
                BigDecimal rate = nvl(o.getReturnShippingChargeRate());
                BigDecimal extra = R.multiply(rate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                debt = debt.add(extra);

                // snapshot returnShippingCharge
                if (o.getReturnShippingCharge() == null || o.getReturnShippingCharge().compareTo(extra) != 0) {
                    o.setReturnShippingCharge(extra);
                }
            }

            // ✅ Chỉ update DB nếu totalDebtOrder thay đổi
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

        // ✅ ĐÃ THANH TOÁN NỢ -> BỎ QUA, KHÔNG TÍNH LẠI
        if (Boolean.TRUE.equals(o.getPaidByShop())) return null;

        // Chỉ tính khi có status
        if (status == null) return null;

        BigDecimal R = nvl(o.getShippingFeeReal());
        if (R.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal E = nvl(o.getShippingFee());

        BigDecimal debt;

        switch (status) {

            case SHIPPING:
            case OUT_FOR_DELIVERY:
            case DELIVERED_WAITING_CONFIRM:
                debt = R;
                break;

            case RETURNING:
                debt = R.multiply(new BigDecimal("1.5"))
                        .setScale(2, RoundingMode.HALF_UP);
                break;

            case DELIVERY_SUCCESS:
                debt = R.subtract(E).max(BigDecimal.ZERO);
                break;

            default:
                return null;
        }

        // (Giữ lại logic cũ) Nếu returnChargeApplied true => cộng thêm rate% của R
        if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
            BigDecimal rate = nvl(o.getReturnShippingChargeRate());
            BigDecimal extra = R.multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            debt = debt.add(extra);

            if (o.getReturnShippingCharge() == null || o.getReturnShippingCharge().compareTo(extra) != 0) {
                o.setReturnShippingCharge(extra);
            }
        }

        return debt;
    }



    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
