package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.StoreOrderItem;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionStatus;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.repository.StoreOrderItemRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.repository.StoreWalletTransactionRepository;
import org.example.audio_ecommerce.service.StorePayoutProcessService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorePayoutProcessServiceImpl implements StorePayoutProcessService {

    private final StoreOrderItemRepository storeOrderItemRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> processMyEligiblePayoutItems() {

        UUID storeId = securityUtils.getCurrentStoreId();
        LocalDateTime now = LocalDateTime.now();

        log.info("========== AUTO PAYOUT START ==========");
        log.info("[AUTO PAYOUT] storeId={} at={}", storeId, now);

        // 1) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Store chưa có ví"));

        BigDecimal walletBalanceStart = nz(wallet.getDefaultBalance());
        log.info("[AUTO PAYOUT] walletId={} defaultBalance(start)={}",
                wallet.getWalletId(), walletBalanceStart);

        // 2) Lấy item đủ điều kiện
        List<StoreOrderItem> candidates = storeOrderItemRepository.findEligibleItemsToAutoPayout(storeId);

        log.info("[AUTO PAYOUT] eligible candidates size={}", candidates.size());

        if (candidates.isEmpty()) {
            log.info("[AUTO PAYOUT] No eligible items. End.");
            log.info("========== AUTO PAYOUT END ==========");
            return ResponseEntity.ok(new BaseResponse<>(200, "✅ Không có item nào đủ điều kiện payout", Map.of(
                    "processedCount", 0,
                    "skippedCount", 0,
                    "addedToDefaultBalance", BigDecimal.ZERO,
                    "defaultBalanceAfter", walletBalanceStart
            )));
        }

        // 3) Process từng item
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;

        int skipped = 0;

        List<UUID> processedItemIds = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        BigDecimal runningBalanceBefore = walletBalanceStart;

        for (StoreOrderItem i : candidates) {

            UUID itemId = i.getId();
            String externalRef = "PAYOUT_ITEM:" + itemId;

            // 3.1) chống chạy lại bằng externalRef unique
            boolean existed = storeWalletTransactionRepository.existsByExternalRef(externalRef);
            if (existed) {
                skipped++;
                skippedReasons.add("DUPLICATE_EXTERNAL_REF itemId=" + itemId);
                log.warn("[AUTO PAYOUT] SKIP itemId={} reason=DUPLICATE_EXTERNAL_REF externalRef={}",
                        itemId, externalRef);
                continue;
            }

            BigDecimal gross = nz(i.getFinalLineTotal());
            BigDecimal pct = nz(i.getPlatformFeePercentage());

            // fee = gross * pct / 100
            BigDecimal fee = gross.multiply(pct)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal net = gross.subtract(fee);

            log.info("[AUTO PAYOUT] itemId={} gross={} pct={} fee={} net={}",
                    itemId, gross, pct, fee, net);

            if (net.compareTo(BigDecimal.ZERO) <= 0) {
                skipped++;
                skippedReasons.add("NET_LE_ZERO itemId=" + itemId + " net=" + net);
                log.warn("[AUTO PAYOUT] SKIP itemId={} reason=NET_LE_ZERO net={}", itemId, net);
                continue;
            }

            // 3.2) update snapshot vào item
            i.setPlatformFeeAmount(fee);
            i.setNetPayoutItem(net);

            // 3.3) mark payout
            i.setIsPayout(true);
            i.setPayoutProcessed(true);

            // 3.4) build transaction per item (trace theo itemId)
            BigDecimal runningBalanceAfter = runningBalanceBefore.add(net);

            StoreWalletTransaction tx = StoreWalletTransaction.builder()
                    .wallet(wallet)
                    .type(StoreWalletTransactionType.DEPOSIT) // hoặc PAYOUT nếu bạn muốn tạo enum mới
                    .status(StoreWalletTransactionStatus.SUCCESS)
                    .amount(net)
                    .balanceBefore(runningBalanceBefore)
                    .balanceAfter(runningBalanceAfter)
                    .orderId(null)
                    .externalRef(externalRef) // ✅ unique theo itemId
                    .description("AUTO PAYOUT orderItem -> defaultBalance"
                            + " | itemId=" + itemId
                            + " | gross=" + gross
                            + " | fee=" + fee
                            + " | net=" + net)
                    .createdAt(now)
                    .build();

            // ✅ LƯU TRANSACTION
            StoreWalletTransaction savedTx = storeWalletTransactionRepository.save(tx);

            // ✅ LOG để biết đã insert DB thành công
            log.info("[AUTO PAYOUT] SAVED TX transactionId={} externalRef={} amount={} balanceBefore={} balanceAfter={}",
                    savedTx.getTransactionId(), externalRef, net, runningBalanceBefore, runningBalanceAfter);

            // 3.5) cộng dồn tổng
            totalGross = totalGross.add(gross);
            totalFee = totalFee.add(fee);
            totalNet = totalNet.add(net);

            processedItemIds.add(itemId);

            // 3.6) cập nhật running balance cho item tiếp theo
            runningBalanceBefore = runningBalanceAfter;
        }

        // 4) Save items (chỉ lưu list candidates vì đã update flag trong đó)
        storeOrderItemRepository.saveAll(candidates);

        // 5) Update wallet (cộng 1 lần theo totalNet)
        BigDecimal walletBalanceEnd = walletBalanceStart.add(totalNet);
        wallet.setDefaultBalance(walletBalanceEnd);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        log.info("[AUTO PAYOUT] SUMMARY processed={} skipped={} totalGross={} totalFee={} totalNet={}",
                processedItemIds.size(), skipped, totalGross, totalFee, totalNet);

        log.info("[AUTO PAYOUT] walletId={} defaultBalance(end)={}",
                wallet.getWalletId(), walletBalanceEnd);

        log.info("========== AUTO PAYOUT END ==========");

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Payout thành công các item đủ điều kiện",
                Map.ofEntries(
                        Map.entry("storeId", storeId),
                        Map.entry("processedCount", processedItemIds.size()),
                        Map.entry("processedItemIds", processedItemIds),
                        Map.entry("skippedCount", skipped),
                        Map.entry("skippedReasons", skippedReasons),
                        Map.entry("totalGross", totalGross),
                        Map.entry("totalPlatformFee", totalFee),
                        Map.entry("addedToDefaultBalance", totalNet),
                        Map.entry("defaultBalanceBefore", walletBalanceStart),
                        Map.entry("defaultBalanceAfter", walletBalanceEnd),
                        Map.entry("ranAt", now)
                )
        ));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }


}
