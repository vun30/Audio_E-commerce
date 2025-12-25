package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegalPointScheduler {

    private final StoreOrderRepository storeOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
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
        increaseSellCountForDeliveredOrders();
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
                        OrderStatus.DELIVERY_FAIL
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

    /**
     * ✅ NEW: DELIVERY_SUCCESS -> +sellCount theo quantity bán (mỗi StoreOrder chỉ cộng 1 lần)
     */
    private void increaseSellCountForDeliveredOrders() {
        List<StoreOrder> orders =
                storeOrderRepository.findByStatusAndSellCountUpdatedFalse(OrderStatus.DELIVERY_SUCCESS);

        if (orders.isEmpty()) return;

        for (StoreOrder so : orders) {
            // TODO: tuỳ entity của bạn, lấy danh sách item của StoreOrder
            // giả sử: so.getItems() trả List<StoreOrderItem>
            if (so.getItems() == null || so.getItems().isEmpty()) {
                so.setSellCountUpdated(true);
                continue;
            }

            // 1) gom productId -> totalQty
            Map<UUID, Integer> qtyByProductId = new HashMap<>();
            for (StoreOrderItem item : so.getItems()) {
                UUID productId = item.getRefId();     // hoặc item.getRefId() nếu refId là productId
                int qty = item.getQuantity();

                if (productId == null || qty <= 0) continue;
                qtyByProductId.merge(productId, qty, Integer::sum);
            }

            if (!qtyByProductId.isEmpty()) {
                // 2) load products 1 lần
                List<Product> products = productRepository.findAllById(qtyByProductId.keySet());

                // 3) + sellCount
                for (Product p : products) {
                    int oldCount = nvlInt(p.getSellCount());
                    int add = nvlInt(qtyByProductId.get(p.getProductId()));
                    p.setSellCount(oldCount + add);

                    log.info("[SELL_COUNT][+{}] productId={} old={} new={} storeOrderId={}",
                            add, p.getProductId(), oldCount, p.getSellCount(), so.getId());
                }

                productRepository.saveAll(products);
            }

            // 4) đánh dấu đã cộng để cron không cộng lại
            so.setSellCountUpdated(true);
        }
    }

    private int nvlInt(Integer v) {
        return v == null ? 0 : v;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
