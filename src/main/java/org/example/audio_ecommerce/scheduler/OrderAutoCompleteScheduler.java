package org.example.audio_ecommerce.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoCompleteScheduler {

    private final CustomerOrderRepository customerOrderRepository;
    private final StoreOrderRepository storeOrderRepository;

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void autoCompleteDeliveredOrders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(24); //chỉnh ở chỗ này để test nhanh hơn

        // 1️⃣ Lấy các CustomerOrder đã DELIVERED >= 24h
        List<CustomerOrder> orders = customerOrderRepository
                .findByStatusAndDeliveredAtBefore(OrderStatus.DELIVERY_SUCCESS, cutoff);

        if (orders.isEmpty()) return;

        for (CustomerOrder order : orders) {

            // idempotent: nếu đã COMPLETED thì bỏ
            if (order.getStatus() == OrderStatus.COMPLETED) continue;

            // 2️⃣ Update tất cả StoreOrder con
            List<StoreOrder> storeOrders =
                    storeOrderRepository.findAllByCustomerOrder_Id(order.getId());

            for (StoreOrder so : storeOrders) {
                if (so.getStatus() == OrderStatus.DELIVERY_SUCCESS) {
                    so.setStatus(OrderStatus.COMPLETED);
                }
            }

            // 3️⃣ Update CustomerOrder
            order.setStatus(OrderStatus.COMPLETED);

            log.info("[AUTO COMPLETE] Order {} completed after 24h delivery",
                    order.getOrderCode());
        }
    }
}
