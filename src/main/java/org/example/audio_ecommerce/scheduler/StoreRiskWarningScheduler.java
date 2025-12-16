package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.service.StoreRiskWarningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreRiskWarningScheduler {

    private final StoreRiskWarningService storeRiskWarningService;

    @Scheduled(cron = "0 */10 * * * *") // mỗi 10 phút
    public void earlyWarningScan() {
        int count = storeRiskWarningService.runEarlyWarningScan();
        log.info("[RiskWarningScheduler] updatedWarnings={}", count);
    }
}