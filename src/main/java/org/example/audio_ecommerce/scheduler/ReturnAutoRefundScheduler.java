package org.example.audio_ecommerce.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.audio_ecommerce.entity.Enum.ReturnStatus;
import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.repository.ReturnRequestRepository;
import org.example.audio_ecommerce.service.WalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnAutoRefundScheduler {

    private final ReturnRequestRepository returnRepo;
    private final WalletService walletService;

    /**
     * Chạy mỗi 30 phút
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void autoRefundDeliveredReturns() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<ReturnRequest> candidates =
                returnRepo.findAutoRefundCandidates(
                        ReturnStatus.DELIVERED,
                        cutoff
                );

        for (ReturnRequest r : candidates) {
            try {
                // Guard an toàn
                if (r.isFinalDecision()) {
                    continue;
                }

                // Refund tiền (idempotent)
                walletService.refundForReturn(r);

                // Update status
                r.setStatus(ReturnStatus.AUTO_REFUNDED);
                r.setUpdatedAt(LocalDateTime.now());

                returnRepo.save(r);

                log.info("AUTO_REFUND_SUCCESS returnRequestId={}", r.getId());

            } catch (Exception ex) {
                log.error("AUTO_REFUND_FAIL returnRequestId={} error={}",
                        r.getId(), ex.getMessage(), ex);
            }
        }
    }
}

