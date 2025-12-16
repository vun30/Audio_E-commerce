package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
