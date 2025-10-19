package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.CustomerOrder;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.service.Impl.SettlementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletReleaseScheduler {
    private final PlatformTransactionRepository platformTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final SettlementService settlementService;

    /** Chạy mỗi ngày lúc 01:00 sáng */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void releaseHeldFunds() {
        LocalDateTime threshold = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        log.info("🔍 [Scheduler] Quét release các giao dịch HOLDING trước {}", threshold);

        List<PlatformTransaction> holdingTxs = platformTransactionRepository.findExpiredHoldings(threshold);
        if (holdingTxs.isEmpty()) {
            log.info("✅ Không có giao dịch cần release.");
            return;
        }

        for (var tx : holdingTxs) {
            CustomerOrder order = customerOrderRepository.findById(tx.getOrderId()).orElse(null);
            if (order == null) continue;

            // Nếu đơn đã hủy / chưa thanh toán / refund thì bỏ qua
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.UNPAID) continue;

            try {
                settlementService.releaseAfterHold(order);
                log.info("💸 Released orderId={} amount={}", order.getId(), tx.getAmount());
            } catch (Exception e) {
                log.error("❌ Release thất bại orderId={}: {}", order.getId(), e.getMessage());
            }
        }
        log.info("🏁 [Scheduler] Hoàn tất quét release.");
    }
}
