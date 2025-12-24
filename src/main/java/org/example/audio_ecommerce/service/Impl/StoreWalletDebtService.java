package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreWalletDebtService {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletRepository storeWalletRepository;

    /**
     * Tính lại tổng nợ (debtBalance) cho 1 store và update vào StoreWallet.
     * Công thức:
     *   newDebt = SUM(StoreOrder.totalDebtOrder của store) + SUM(ReturnShippingFee của store)
     */
    @Transactional
    public void recalcStoreDebtBalanceByStoreId(UUID storeId) {
        if (storeId == null) return;

        // 1) Sum nợ từ StoreOrder
        BigDecimal orderDebt = nz(storeOrderRepository.sumDebtOrdersByStoreId(storeId));

        // 2) Sum nợ từ ReturnShippingFee
        BigDecimal returnDebt = nz(returnShippingFeeRepository.sumDebtReturnFeesByStoreId(storeId));

        BigDecimal newDebt = orderDebt.add(returnDebt);

        // 3) Update StoreWallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId).orElse(null);
        if (wallet == null) {
            log.warn("[WALLET-DEBT][SKIP] storeId={} wallet not found", storeId);
            return;
        }

        BigDecimal oldDebt = nz(wallet.getDebtBalance());

        if (oldDebt.compareTo(newDebt) != 0) {
            wallet.setDebtBalance(newDebt);
            storeWalletRepository.save(wallet);

            log.info("[WALLET-DEBT] storeId={} oldDebt={} newDebt={}", storeId, oldDebt, newDebt);
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
