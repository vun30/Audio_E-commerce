package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.PlatformTransaction;
import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.repository.PlatformWalletRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.AutoCancelOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCancelOrderScheduler {

    private final StoreOrderRepository storeOrderRepo;
    private final AutoCancelOrderService autoCancelOrderService;

    @Scheduled(fixedDelay = 60_000) // mỗi 1 phút
    public void autoCancel() {
        LocalDateTime now = LocalDateTime.now();

        // RULE 1: quá 12h không confirm
        LocalDateTime createdDeadline = now.minusHours(12);
        List<StoreOrder> notConfirmed = storeOrderRepo
                .findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, createdDeadline);

        for (StoreOrder so : notConfirmed) {
            try {
                autoCancelOrderService.autoCancelWholeCustomerOrder(
                        so,
                        "STORE_NOT_CONFIRM_OVER_12H",
                        "Shop quá 12 tiếng chưa xác nhận đơn. Hệ thống tự huỷ."
                );
            } catch (Exception e) {
                log.error("Auto-cancel 12h failed storeOrder={}", so.getId(), e);
            }
        }

        // RULE 2: quá 24h sau confirm mà chưa bàn giao GHN
        LocalDateTime confirmDeadline = now.minusHours(24);
        List<StoreOrder> notHanded = storeOrderRepo.findNeedAutoCancelAfterConfirm(
                OrderStatus.AWAITING_SHIPMENT, // status sau confirm (đổi đúng enum bạn đang dùng)
                confirmDeadline
        );

        for (StoreOrder so : notHanded) {
            try {
                autoCancelOrderService.autoCancelWholeCustomerOrder(
                        so,
                        "STORE_NOT_HANDOVER_GHN_OVER_24H",
                        "Shop quá 24 tiếng chưa bàn giao GHN. Hệ thống tự huỷ."
                );
            } catch (Exception e) {
                log.error("Auto-cancel 24h failed storeOrder={}", so.getId(), e);
            }
        }
    }
}
