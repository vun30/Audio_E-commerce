package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.service.Impl.GhnStatusSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnOrderStatusScheduler {

    private final GhnStatusSyncService ghnStatusSyncService;

    @Scheduled(cron = "0 */1 * * * ?") //Test nhanh: "0 */1 * * * ?" (mỗi phút)
    @Transactional
    public void syncGhnOrderStatuses() {
        log.info("⏱ [Scheduler] Bắt đầu đồng bộ trạng thái GHN orders.");
        ghnStatusSyncService.syncAllActiveOrders();
        log.info("✅ [Scheduler] Kết thúc đồng bộ trạng thái GHN orders.");
    }
}
