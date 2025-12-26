package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.scheduler.StoreDebtUnlockService;
import org.example.audio_ecommerce.scheduler.StoreWalletDebtCron;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreDebtPaymentService {

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final StoreWalletTransactionRepository  storeWalletTransactionRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final StoreWalletDebtCron storeWalletDebtCron;
    private final StoreDebtUnlockService storeDebtUnlockService;

    @Transactional
    public void payDebtForStore(UUID storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Store wallet not found: " + storeId));

        // 1️⃣ unpaid FINAL orders
        List<StoreOrder> unpaidFinalOrders =
                storeOrderRepository.findUnpaidFinalOrdersOfStore(storeId);

        // 2️⃣ unpaid return shipping fees
        List<ReturnShippingFee> unpaidReturnFees =
                returnShippingFeeRepository.findUnpaidShopReturnFees(storeId);

        BigDecimal totalOrderDebt = unpaidFinalOrders.stream()
                .map(o -> nz(o.getTotalDebtOrder()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnFeeDebt = unpaidReturnFees.stream()
                .map(f -> nz(
                        nz(f.getChargedToShop()).compareTo(BigDecimal.ZERO) > 0
                                ? f.getChargedToShop()
                                : f.getShippingFee()
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalToPay = totalOrderDebt.add(totalReturnFeeDebt);

        if (totalToPay.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("✅ Store {} không có nợ", storeId);
            return;
        }

        BigDecimal balance = nz(wallet.getDefaultBalance());
        if (balance.compareTo(totalToPay) < 0) {
            log.warn("❌ Store {} không đủ tiền để auto-pay. Cần={}, có={}",
                    storeId, totalToPay, balance);
            return; // ❗ KHÔNG throw để cron tiếp tục store khác
        }

        LocalDateTime now = LocalDateTime.now();

        // 3️⃣ Trừ tiền store wallet
        BigDecimal after = balance.subtract(totalToPay);
        wallet.setDefaultBalance(after);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 4️⃣ Store wallet tx
        storeWalletTransactionRepository.save(
                StoreWalletTransaction.builder()
                        .wallet(wallet)
                        .type(StoreWalletTransactionType.DEBT_PAYMENT)
                        .amount(totalToPay)
                        .balanceAfter(after)
                        .description("Auto monthly debt payment")
                        .createdAt(now)
                        .build()
        );

        // 5️⃣ Platform ledger only
        PlatformWallet platformWallet = platformWalletRepository
                .findMainPlatformWallet()
                .orElseThrow(() -> new RuntimeException("PlatformWallet not found"));

        platformTransactionRepository.save(
                PlatformTransaction.builder()
                        .wallet(platformWallet)
                        .storeId(storeId)
                        .amount(totalToPay)
                        .type(TransactionType.DEBT_PAYMENT)
                        .status(TransactionStatus.SUCCESS)
                        .direction(TxDirection.IN)
                        .bucket(WalletBucket.CASH)
                        .balanceBefore(platformWallet.getCashBalance())
                        .balanceAfter(platformWallet.getCashBalance())
                        .description("Auto collect store debt")
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        // 6️⃣ mark paid
        unpaidFinalOrders.forEach(o -> o.setPaidByShop(true));
        unpaidReturnFees.forEach(f -> f.setPaidByShop(true));

        storeOrderRepository.saveAll(unpaidFinalOrders);
        returnShippingFeeRepository.saveAll(unpaidReturnFees);

        storeWalletDebtCron.recalcStoreDebtBalanceByStoreId(storeId);
        storeDebtUnlockService.tryUnlockStore(storeId);

        log.info("✅ Auto paid debt for store {} amount={}", storeId, totalToPay);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
