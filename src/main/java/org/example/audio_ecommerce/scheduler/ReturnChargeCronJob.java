package org.example.audio_ecommerce.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnChargeCronJob {

    private final StoreOrderRepository storeOrderRepository;

    /**
     * Chạy mỗi 5 phút
     */
//@Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void applyReturnChargeForReturningOrders() {

        List<StoreOrder> orders =
                storeOrderRepository.findByStatusAndReturnChargeApplied(
                        OrderStatus.RETURNING,
                        false
                );

        if (orders.isEmpty()) {
            return;
        }

        log.info("[CRON][RETURN_CHARGE] Found {} returning orders", orders.size());

        for (StoreOrder order : orders) {
            try {
                applyReturnCharge(order);
            } catch (Exception e) {
                log.error("[CRON][RETURN_CHARGE] Failed for order {}",
                        order.getId(), e);
            }
        }
    }

    private void applyReturnCharge(StoreOrder order) {

        // double safety (idempotent)
        if (Boolean.TRUE.equals(order.getReturnChargeApplied())) {
            return;
        }

        BigDecimal shippingReal =
                Optional.ofNullable(order.getShippingFeeReal())
                        .orElse(BigDecimal.ZERO);

        BigDecimal rate = order.getReturnShippingChargeRate()
                .divide(BigDecimal.valueOf(100)); // 50% -> 0.5

        BigDecimal returnCharge = shippingReal.multiply(rate);

        order.setReturnShippingCharge(returnCharge);

        order.setTotalDebtOrder(
                order.getTotalDebtOrder().add(returnCharge)
        );

        order.setReturnChargeApplied(true);
        order.setReturnChargeAppliedAt(LocalDateTime.now());

        log.info("[CRON][RETURN_CHARGE] Applied {} for order {}",
                returnCharge, order.getId());
    }
}

