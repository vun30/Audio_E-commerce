package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreWalletDebtCron {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletRepository storeWalletRepository;

    @Scheduled(cron = "*/30 * * * * *") // mỗi 2 phút
    @Transactional
    public void recalcStoreDebtBalance() {

        Map<UUID, BigDecimal> debtMap = new HashMap<>();

        // 1️⃣ Nợ từ StoreOrder
        for (Object[] row : storeOrderRepository.sumDebtFromOrdersByStore()) {
            UUID storeId = (UUID) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            debtMap.put(storeId, amount);
        }

        // 2️⃣ Nợ từ ReturnShippingFee
        for (Object[] row : returnShippingFeeRepository.sumDebtFromReturnFeesByStore()) {
            UUID storeId = (UUID) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            debtMap.merge(storeId, amount, BigDecimal::add);
        }

        // 3️⃣ Update StoreWallet
        int updated = 0;
        for (Map.Entry<UUID, BigDecimal> entry : debtMap.entrySet()) {
            UUID storeId = entry.getKey();
            BigDecimal newDebt = entry.getValue();

            StoreWallet wallet = storeWalletRepository
                    .findByStore_StoreId(storeId)
                    .orElse(null);

            if (wallet == null) continue;

            if (wallet.getDebtBalance().compareTo(newDebt) != 0) {
                wallet.setDebtBalance(newDebt);
                storeWalletRepository.save(wallet);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("StoreWalletDebtCron updated debtBalance for {} stores", updated);
        }
    }
}
