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

    @Scheduled(cron = "*/10 * * * * *")
    @Transactional
    public void recalcStoreDebtBalance() {

        Map<UUID, BigDecimal> debtMap = new HashMap<>();

        for (Object[] row : storeOrderRepository.sumDebtFromOrdersByStore()) {
            debtMap.put((UUID) row[0], nz((BigDecimal) row[1]));
        }

        for (Object[] row : returnShippingFeeRepository.sumDebtFromReturnFeesByStore()) {
            debtMap.merge((UUID) row[0], nz((BigDecimal) row[1]), BigDecimal::add);
        }

        int updated = 0;

        // ✅ cập nhật tất cả store wallet (kể cả về 0)
        List<StoreWallet> wallets = storeWalletRepository.findAll();

        for (StoreWallet w : wallets) {
            UUID storeId = w.getStore().getStoreId(); // tùy entity bạn
            BigDecimal newDebt = debtMap.getOrDefault(storeId, BigDecimal.ZERO);

            BigDecimal oldDebt = nz(w.getDebtBalance());
            if (oldDebt.compareTo(newDebt) != 0) {
                w.setDebtBalance(newDebt);
                updated++;
            }
        }

        if (updated > 0) {
            storeWalletRepository.saveAll(wallets);
            log.info("updated debtBalance for {} stores", updated);
        }
    }

    //    @Transactional
//    public void recalcStoreDebtBalanceByStoreId(UUID storeId) {
//
//        if (storeId == null) return;
//
//        // 1️⃣ Nợ từ StoreOrder (reuse y hệt cron, nhưng chỉ lấy phần của storeId)
//        BigDecimal orderDebt = BigDecimal.ZERO;
//        for (Object[] row : storeOrderRepository.sumDebtFromOrdersByStore()) {
//            UUID sid = (UUID) row[0];
//            if (storeId.equals(sid)) {
//                orderDebt = (BigDecimal) row[1];
//                break;
//            }
//        }
//
//        // 2️⃣ Nợ từ ReturnShippingFee (reuse y hệt cron, nhưng chỉ lấy phần của storeId)
//        BigDecimal returnDebt = BigDecimal.ZERO;
//        for (Object[] row : returnShippingFeeRepository.sumDebtFromReturnFeesByStore()) {
//            UUID sid = (UUID) row[0];
//            if (storeId.equals(sid)) {
//                returnDebt = (BigDecimal) row[1];
//                break;
//            }
//        }
//
//        BigDecimal newDebt = nz(orderDebt).add(nz(returnDebt));
//
//        // 3️⃣ Update StoreWallet (chỉ store này)
//        StoreWallet wallet = storeWalletRepository
//                .findByStore_StoreId(storeId)
//                .orElse(null);
//
//        if (wallet == null) {
//            log.warn("[DEBT-RECALC-ONE][SKIP] storeId={} wallet not found", storeId);
//            return;
//        }
//
//        BigDecimal oldDebt = nz(wallet.getDebtBalance());
//
//        if (oldDebt.compareTo(newDebt) != 0) {
//            wallet.setDebtBalance(newDebt);
//            storeWalletRepository.save(wallet);
//
//            log.info("[DEBT-RECALC-ONE] storeId={} oldDebt={} newDebt={}",
//                    storeId, oldDebt, newDebt);
//        }
//    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional
    public void recalcStoreDebtBalanceByStoreId(UUID storeId) {
        if (storeId == null) return;

        BigDecimal orderDebt = nz(storeOrderRepository.sumDebtOrdersByStoreId(storeId));
        BigDecimal returnDebt = nz(returnShippingFeeRepository.sumDebtReturnFeesByStoreId(storeId));

        BigDecimal newDebt = orderDebt.add(returnDebt);

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId).orElse(null);
        if (wallet == null) {
            log.warn("[DEBT-RECALC] storeId={} wallet not found", storeId);
            return;
        }

        BigDecimal oldDebt = nz(wallet.getDebtBalance());
        if (oldDebt.compareTo(newDebt) != 0) {
            wallet.setDebtBalance(newDebt);
            storeWalletRepository.save(wallet);

            log.info("[DEBT-RECALC] storeId={} oldDebt={} newDebt={}", storeId, oldDebt, newDebt);
        }
    }

}
