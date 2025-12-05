package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnRequestScheduler {

    private final ReturnRequestService returnRequestService;

    @Scheduled(cron = "0 */1 * * * *")
    public void handleTimeouts() {
        returnRequestService.autoApprovePendingReturns();
        returnRequestService.autoCancelUnshippedReturns();
        returnRequestService.autoRefundForUnresponsiveShop();
        returnRequestService.autoHandleGhnPickupTimeout();
    }
}
