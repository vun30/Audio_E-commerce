package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.Impl.StoreDebtPaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyStoreDebtCron {

    private final StoreRepository  storeRepository;
    private final StoreDebtPaymentService storeDebtPaymentService;

    /**
     * ⏰ Chạy lúc 02:00 sáng ngày 1 hàng tháng
     * Cron: second minute hour day month dayOfWeek
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void autoCollectStoreDebtMonthly() {

        log.info("🚀 START monthly store debt collection");

        List<UUID> storeIds = storeRepository.findAllActiveStoreIds();

        for (UUID storeId : storeIds) {
            try {
                storeDebtPaymentService.payDebtForStore(storeId);
            } catch (Exception ex) {
                // ❗ Không fail toàn cron
                log.error("❌ Auto collect debt failed for store {}",
                        storeId, ex);
            }
        }

        log.info("✅ FINISH monthly store debt collection");
    }
}