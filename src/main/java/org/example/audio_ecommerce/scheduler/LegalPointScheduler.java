package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegalPointScheduler {

    private final StoreOrderRepository storeOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;

    /**
     * Chạy mỗi 5 phút
     * - StoreOrder DELIVERED  -> +1 legalPoint cho Store
     * - CustomerOrder RETURNING -> -1 legalPoint cho Customer
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void execute() {
        rewardStoreForDeliveredOrders();
        rewardCustomerForDeliveredOrders();
        penalizeCustomerForReturningOrders();
    }

    /**
     * ✅ Store +1 legalPoint nếu đơn đã DELIVERED
     */
    private void rewardStoreForDeliveredOrders() {
        List<StoreOrder> orders =
                storeOrderRepository.findByStatusAndStoreScoredFalse(
                        OrderStatus.DELIVERY_SUCCESS
                );

        if (orders.isEmpty()) return;

        for (StoreOrder so : orders) {
            Store store = so.getStore();
            if (store == null) continue;

            BigDecimal current = nvl(store.getLegalPoint());
            store.setLegalPoint(current.add(BigDecimal.ONE));

            so.setStoreScored(true);

            log.info(
                    "[LEGAL_POINT][STORE +1] storeId={} storeOrderId={} newLegalPoint={}",
                    store.getStoreId(),
                    so.getId(),
                    store.getLegalPoint()
            );
        }
    }

    /**
     * ❌ Customer -1 legalPoint nếu đơn RETURNING
     */
    private void penalizeCustomerForReturningOrders() {
        List<CustomerOrder> orders =
                customerOrderRepository.findByStatusAndCustomerPenalizedFalse(
                        OrderStatus.RETURNING
                );

        if (orders.isEmpty()) return;

        for (CustomerOrder co : orders) {
            if (co.getCustomer() == null) continue;

            BigDecimal current = nvl(co.getCustomer().getLegalPoint());
            co.getCustomer().setLegalPoint(current.subtract(BigDecimal.ONE));

            co.setCustomerPenalized(true);

            log.info(
                    "[LEGAL_POINT][CUSTOMER -1] customerId={} orderId={} newLegalPoint={}",
                    co.getCustomer().getId(),
                    co.getId(),
                    co.getCustomer().getLegalPoint()
            );
        }
    }

    /**
     * ✅ Customer +1 legalPoint nếu đơn DELIVERED
     */
    private void rewardCustomerForDeliveredOrders() {
        List<CustomerOrder> orders =
                customerOrderRepository
                        .findByStatusAndCustomerRewardedFalse(
                                OrderStatus.DELIVERY_SUCCESS
                        );

        if (orders.isEmpty()) return;

        for (CustomerOrder co : orders) {
            if (co.getCustomer() == null) continue;

            BigDecimal current = nvl(co.getCustomer().getLegalPoint());
            co.getCustomer().setLegalPoint(current.add(BigDecimal.ONE));

            co.setCustomerRewarded(true);

            log.info(
                    "[LEGAL_POINT][CUSTOMER +1] customerId={} orderId={} newLegalPoint={}",
                    co.getCustomer().getId(),
                    co.getId(),
                    co.getCustomer().getLegalPoint()
            );
        }
    }


    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
