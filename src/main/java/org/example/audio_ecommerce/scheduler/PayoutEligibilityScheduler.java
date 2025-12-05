package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.service.Impl.PayoutEligibilityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutEligibilityScheduler {

    private final PayoutEligibilityService payoutEligibilityService;

    // ✅ Chạy tự động mỗi 2 giây
    @Scheduled(fixedRate = 2000) 
    public void runScheduler() {
        payoutEligibilityService.evaluateEligibility();
        payoutEligibilityService.checkReturnedItems();    // kiểm tra return success
        payoutEligibilityService.syncDeliveredAtForItems();
        payoutEligibilityService.calculateShippingFeeDifference();
    }


}
