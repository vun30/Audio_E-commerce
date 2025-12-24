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

            BigDecimal R = nvl(o.getShippingFeeReal());
            if (R.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal E = nvl(o.getShippingFee());

            // 1) Base debt
            BigDecimal debt = (o.getDeliveredAt() != null)
                    ? R.subtract(E).max(BigDecimal.ZERO)
                    : R;

            // 2) Nếu returnChargeApplied true => cộng thêm rate% của R
            if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
                BigDecimal rate = nvl(o.getReturnShippingChargeRate()); // ví dụ 50.00
                BigDecimal extra = R.multiply(rate).divide(new BigDecimal("100"));

                debt = debt.add(extra);

                // snapshot returnShippingCharge
                if (o.getReturnShippingCharge() == null || o.getReturnShippingCharge().compareTo(extra) != 0) {
                    o.setReturnShippingCharge(extra);
                }
            }

            // 3) Chỉ update DB nếu totalDebtOrder thay đổi
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

    @Transactional
    public void recalcDebtForOrder(UUID storeOrderId, OrderStatus status) {

        StoreOrder o = storeOrderRepository.findById(storeOrderId)
                .orElseThrow(() -> new RuntimeException("❌ StoreOrder not found: " + storeOrderId));

        BigDecimal newDebt = computeDebt(o, status);

        // Nếu computeDebt trả null => nghĩa là R<=0 => không update gì
        if (newDebt == null) return;

        boolean changed = (o.getTotalDebtOrder() == null || o.getTotalDebtOrder().compareTo(newDebt) != 0);

        if (changed) {
            o.setTotalDebtOrder(newDebt);
            storeOrderRepository.save(o);
            log.info("recalcDebtForOrder updated order={} debt={}", storeOrderId, newDebt);
        }
    }

    /**
     * TÍNH NỢ - giữ đúng logic cron.
     * Trả về:
     *  - null nếu không cần update (R<=0)
     *  - debt nếu có thể tính
     */
    private BigDecimal computeDebt(StoreOrder o, OrderStatus status) {

        BigDecimal R = nvl(o.getShippingFeeReal());
        if (R.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal E = nvl(o.getShippingFee());

        // delivered hay chưa? (chọn 1 trong 2 cách)
        // Cách A (khớp 100% cron): dựa vào deliveredAt
        boolean delivered = (o.getDeliveredAt() != null);

        // Cách B (nếu bạn muốn status quyết định): bật dòng dưới và tắt dòng trên
        // boolean delivered = isDeliveredStatus(status);

        // 1) Base debt
        BigDecimal debt = delivered
                ? R.subtract(E).max(BigDecimal.ZERO)
                : R;

        // 2) return charge
        if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
            BigDecimal rate = nvl(o.getReturnShippingChargeRate()); // ví dụ 50.00
            BigDecimal extra = R.multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            debt = debt.add(extra);

            // snapshot returnShippingCharge
            if (o.getReturnShippingCharge() == null || o.getReturnShippingCharge().compareTo(extra) != 0) {
                o.setReturnShippingCharge(extra);
            }
        }

        return debt;
    }

    // Nếu bạn dùng Cách B theo status, map status delivered ở đây
    private boolean isDeliveredStatus(OrderStatus status) {
        if (status == null) return false;
        // sửa theo enum status thực tế của bạn
        return status == OrderStatus.DELIVERY_SUCCESS
                || status == OrderStatus.COMPLETED;
    }


    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
