package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.service.PlatformCampaignService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformCampaignScheduler {

    private final PlatformCampaignService platformCampaignService;

    // ✅ Chạy tự động mỗi phút
    @Scheduled(fixedRate = 6000)
    public void runScheduler() {
        platformCampaignService.tickAllCampaigns();
    }
}
