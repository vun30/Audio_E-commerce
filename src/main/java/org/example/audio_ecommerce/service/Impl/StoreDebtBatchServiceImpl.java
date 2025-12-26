package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.service.StoreDebtBatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreDebtBatchServiceImpl implements StoreDebtBatchService {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletRepository storeWalletRepository;

    @Override
    @Transactional
    public int recalcAllStoresDebt() {

        Map<UUID, BigDecimal> debtMap = new HashMap<>();

        // 1) debt từ StoreOrder
        for (Object[] row : storeOrderRepository.sumDebtFromOrdersByStore()) {
            UUID storeId = (UUID) row[0];
            BigDecimal amount = nz((BigDecimal) row[1]);
            debtMap.put(storeId, amount);
        }

        // 2) debt từ ReturnShippingFee
        for (Object[] row : returnShippingFeeRepository.sumDebtFromReturnFeesByStore()) {
            UUID storeId = (UUID) row[0];
            BigDecimal amount = nz((BigDecimal) row[1]);
            debtMap.merge(storeId, amount, BigDecimal::add);
        }

        // ✅ Load tất cả wallets để set về 0 cho store không còn nợ
        List<StoreWallet> wallets = storeWalletRepository.findAll();
        if (wallets.isEmpty()) return 0;

        int updated = 0;
        List<StoreWallet> changed = new ArrayList<>();

        for (StoreWallet w : wallets) {
            if (w.getStore() == null || w.getStore().getStoreId() == null) continue;

            UUID storeId = w.getStore().getStoreId();

            // ✅ store không có trong debtMap => nợ = 0
            BigDecimal newDebt = debtMap.getOrDefault(storeId, BigDecimal.ZERO);
            BigDecimal oldDebt = nz(w.getDebtBalance());

            if (oldDebt.compareTo(newDebt) != 0) {
                w.setDebtBalance(newDebt);
                changed.add(w);
                updated++;
            }
        }

        if (!changed.isEmpty()) {
            storeWalletRepository.saveAll(changed);
            log.info("[DEBT-RECALC-ALL] updated={} stores", updated);
        }

        return updated;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
