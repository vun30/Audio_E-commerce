package org.example.audio_ecommerce.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreScoreScheduler {

    private final StoreOrderRepository storeOrderRepository;

    /**
     * Chạy mỗi 5 phút
     * - StoreOrder DELIVERY_SUCCESS
     * - chưa storeScored
     * -> set storeScored = true
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void markStoreOrderScored() {

        List<StoreOrder> orders =
                storeOrderRepository.findDeliverySuccessNotScored(
                        OrderStatus.DELIVERY_SUCCESS
                );

        if (orders.isEmpty()) return;

        for (StoreOrder so : orders) {
            so.setStoreScored(true);
            log.info("✅ Mark storeScored=true for StoreOrder {}", so.getId());
        }

        storeOrderRepository.saveAll(orders);
    }
}


