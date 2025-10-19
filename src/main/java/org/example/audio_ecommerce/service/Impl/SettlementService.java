package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PlatformWalletRepository platformWalletRepo;
    private final PlatformTransactionRepository platformTxRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final StoreWalletRepository storeWalletRepo;
    private final StoreWalletTransactionRepository storeWalletTxRepo;

    @Transactional
    public void recordCustomerQrPayment(UUID customerId, UUID orderId, BigDecimal amount) {
        Wallet wallet = walletRepo.findByCustomer_Id(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.QR)
                .status(WalletTransactionStatus.SUCCESS)
                .description("PayOS QR for order " + orderId)
                .balanceBefore(wallet.getBalance())      // không thay đổi số dư KH
                .balanceAfter(wallet.getBalance())
                .orderId(orderId)
                .build();

        walletTxRepo.save(tx);
        wallet.setLastTransactionAt(java.time.LocalDateTime.now());
        walletRepo.save(wallet);
    }

    @Transactional
    public void moveToPlatformHold(UUID orderId, BigDecimal amount) {
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        plat.setTotalBalance(plat.getTotalBalance().add(amount));
        plat.setPendingBalance(plat.getPendingBalance().add(amount));
        plat.setReceivedTotal(plat.getReceivedTotal().add(amount));
        plat.setUpdatedAt(java.time.LocalDateTime.now());
        platformWalletRepo.save(plat);

        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(orderId)
                .amount(amount)
                .type(TransactionType.HOLD)
                .status(TransactionStatus.PENDING)
                .description("Customer paid – holding 7 days")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        platformTxRepo.save(ptx);
    }

    @Transactional
    public void allocateToStoresPending(CustomerOrder order) {
        Map<UUID, BigDecimal> storeTotals = order.getItems().stream()
                .collect(Collectors.groupingBy(
                        CustomerOrderItem::getStoreId,
                        Collectors.mapping(CustomerOrderItem::getLineTotal,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        for (Map.Entry<UUID, BigDecimal> e : storeTotals.entrySet()) {
            UUID storeId = e.getKey();
            BigDecimal amount = e.getValue();

            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            sw.setPendingBalance(sw.getPendingBalance().add(amount));
            sw.setTotalRevenue(sw.getTotalRevenue().add(amount));
            sw.setUpdatedAt(java.time.LocalDateTime.now());
            storeWalletRepo.save(sw);

            StoreWalletTransaction stx = StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.PENDING_HOLD)
                    .amount(amount)
                    .balanceAfter(sw.getAvailableBalance()) // available chưa đổi
                    .description("Hold 7 days for order " + order.getId())
                    .orderId(order.getId())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            storeWalletTxRepo.save(stx);
        }
    }

    @Transactional
    public void releaseAfterHold(CustomerOrder order) {
        Map<UUID, BigDecimal> storeTotals = order.getItems().stream()
                .collect(Collectors.groupingBy(
                        CustomerOrderItem::getStoreId,
                        Collectors.mapping(CustomerOrderItem::getLineTotal,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        BigDecimal totalRelease = storeTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // platform pending -> done
        plat.setPendingBalance(plat.getPendingBalance().subtract(totalRelease));
        plat.setDoneBalance(plat.getDoneBalance().add(totalRelease));
        plat.setUpdatedAt(java.time.LocalDateTime.now());
        platformWalletRepo.save(plat);

        // update platform transactions of this order -> DONE
        List<PlatformTransaction> ptxs = platformTxRepo.findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING);
        for (PlatformTransaction p : ptxs) {
            p.setStatus(TransactionStatus.DONE);
            p.setUpdatedAt(java.time.LocalDateTime.now());
        }
        platformTxRepo.saveAll(ptxs);

        // move each store pending -> available
        for (Map.Entry<UUID, BigDecimal> e : storeTotals.entrySet()) {
            UUID storeId = e.getKey();
            BigDecimal amount = e.getValue();

            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            sw.setPendingBalance(sw.getPendingBalance().subtract(amount));
            sw.setAvailableBalance(sw.getAvailableBalance().add(amount));
            sw.setUpdatedAt(java.time.LocalDateTime.now());
            storeWalletRepo.save(sw);

            StoreWalletTransaction stx = StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.RELEASE_PENDING)
                    .amount(amount)
                    .balanceAfter(sw.getAvailableBalance())
                    .description("Release after 7 days for order " + order.getId())
                    .orderId(order.getId())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            storeWalletTxRepo.save(stx);
        }
    }
}
