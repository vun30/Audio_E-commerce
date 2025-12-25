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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodCollectedToPlatformWalletJob {

    private final StoreOrderRepository storeOrderRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformTransactionRepository platformTransactionRepository;

    /**
     * Chạy mỗi 5 phút (tuỳ bạn chỉnh).
     * Điều kiện:
     * - StoreOrder status = DELIVERY_SUCCESS (hoặc enum bạn đang dùng cho delivered success)
     * - paymentMethod = COD
     * - codCollected = false (chưa ghi nhận đã thu COD)
     */
    @Scheduled(cron = "0 */5 * * * *") // mỗi 5 phút
    @Transactional
    public void collectCodToPlatformCashBalance() {

        // ✅ ĐỔI enum đúng của bạn tại đây nếu khác:
        OrderStatus deliveredSuccessStatus = OrderStatus.DELIVERY_SUCCESS;

        List<StoreOrder> targets = storeOrderRepository
                .findByStatusAndPaymentMethodAndCodCollectedFalse(deliveredSuccessStatus, PaymentMethod.COD);

        if (targets.isEmpty()) return;

        // Lock ví platform để cộng tiền an toàn
        PlatformWallet platformWallet = platformWalletRepository.getPlatformMainWallet();

        for (StoreOrder so : targets) {
            try {
                // Idempotency key theo order id
                String idem = "COD_COLLECTED:" + so.getId();

                // Nếu transaction đã tồn tại thì skip (phòng trường hợp cron crash giữa chừng)
                if (platformTransactionRepository.findByIdempotencyKey(idem).isPresent()) {
                    so.setCodCollected(true);
                    continue;
                }

                BigDecimal codAmount = safe(so.getGrandTotal()); // ✅ số tiền COD thu hộ
                if (codAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    so.setCodCollected(true); // không thu gì thì vẫn mark tránh chạy lại
                    continue;
                }

                BigDecimal before = safe(platformWallet.getCashBalance());
                BigDecimal after = before.add(codAmount);

                // Cộng tiền vào cashBalance (các field khác bạn muốn sync thì tuỳ chỉnh)
                platformWallet.setCashBalance(after);
                platformWallet.setUpdatedAt(LocalDateTime.now());

                // (Optional) nếu bạn vẫn dùng totalBalance/doneBalance...
                platformWallet.setTotalBalance(safe(platformWallet.getTotalBalance()).add(codAmount));
                platformWallet.setDoneBalance(safe(platformWallet.getDoneBalance()).add(codAmount));
                platformWallet.setReceivedTotal(safe(platformWallet.getReceivedTotal()).add(codAmount));

                PlatformTransaction tx = PlatformTransaction.builder()
                        .wallet(platformWallet)
                        .orderId(so.getId())
                        .storeId(so.getStore() != null ? so.getStore().getStoreId() : null)
                        .amount(codAmount)

                        // META bắt buộc
                        .type(TransactionType.COD_COLLECTED)      // ✅ tạo enum này nếu chưa có
                        .status(TransactionStatus.DONE)
                        .channel(PaymentChannel.COD)
                        .bucket(WalletBucket.CASH)
                        .direction(TxDirection.IN)

                        // SNAPSHOT
                        .balanceBefore(before)
                        .balanceAfter(after)

                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())

                        .idempotencyKey(idem)
                        .externalRefCode(so.getOrderCode())
                        .description("Thu tiền COD cho đơn vận chuyển thành công.")
                        .build();

                platformTransactionRepository.save(tx);

                // Mark đã thu COD để job không chạy lại
                so.setCodCollected(true);

            } catch (Exception e) {
                log.error("[COD JOB] Failed for storeOrderId={}: {}", so.getId(), e.getMessage(), e);
                // không throw để các đơn khác vẫn chạy
            }
        }

        // save changes
        platformWalletRepository.save(platformWallet);
        storeOrderRepository.saveAll(targets);
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
