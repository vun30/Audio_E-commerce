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

    @Transactional
    public void refundEntireOrderToCustomerWallet(CustomerOrder order) {
        // Tổng tiền đơn
        var total = order.getItems().stream()
                .map(CustomerOrderItem::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 1) Platform (refund từ platform → customer)
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // CHANGED: chặn âm & cộng refundedTotal
        plat.setPendingBalance(plat.getPendingBalance().subtract(total).max(java.math.BigDecimal.ZERO));
        plat.setTotalBalance(plat.getTotalBalance().subtract(total));
        plat.setRefundedTotal(plat.getRefundedTotal().add(total));
        plat.setUpdatedAt(java.time.LocalDateTime.now());
        platformWalletRepo.save(plat);

        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(order.getId())
                .amount(total)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .description("Refund entire order to customer")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        platformTxRepo.save(ptx);

        // 2) Gỡ pending từng shop (nếu đã allocate)
        var storeTotals = order.getItems().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CustomerOrderItem::getStoreId,
                        java.util.stream.Collectors.mapping(CustomerOrderItem::getLineTotal,
                                java.util.stream.Collectors.reducing(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                ));

        for (var e : storeTotals.entrySet()) {
            java.util.UUID storeId = e.getKey();
            java.math.BigDecimal amount = e.getValue();

            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            // CHANGED: chỉ hạ pending, không đụng available; chặn âm
            sw.setPendingBalance(sw.getPendingBalance().subtract(amount).max(java.math.BigDecimal.ZERO));
            sw.setUpdatedAt(java.time.LocalDateTime.now());
            storeWalletRepo.save(sw);

            // CHANGED: type chuẩn là PENDING_REVERSED (không phải RELEASE_PENDING)
            StoreWalletTransaction stx = StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.RELEASE_PENDING)
                    .amount(amount)
                    .balanceAfter(sw.getAvailableBalance())
                    .description("Reverse pending due to full order cancel " + order.getId())
                    .orderId(order.getId())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            storeWalletTxRepo.save(stx);
        }

        // 3) Cộng ví khách
        Wallet wallet = walletRepo.findByCustomer_Id(order.getCustomer().getId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));
        var oldBalance = wallet.getBalance();                   // CHANGED: chốt số dư trước
        wallet.setBalance(oldBalance.add(total));
        wallet.setLastTransactionAt(java.time.LocalDateTime.now());
        walletRepo.save(wallet);

        WalletTransaction wtx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(total)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Refund for order " + order.getId())
                .balanceBefore(oldBalance)                      // CHANGED
                .balanceAfter(oldBalance.add(total))            // CHANGED
                .orderId(order.getId())
                .build();
        walletTxRepo.save(wtx);
    }


    /** Refund một PHẦN theo storeOrder (KH đã thanh toán online, đơn đang AWAITING_SHIPMENT, shop duyệt). */
    @Transactional
    public void refundStorePartToCustomerWallet(StoreOrder storeOrder) {
        java.math.BigDecimal amount = storeOrder.getItems().stream()
                .map(StoreOrderItem::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        CustomerOrder order = storeOrder.getCustomerOrder();

        // 1) Trả từ Platform → Customer
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // CHANGED: hạ pending, hạ totalBalance, +refundedTotal, chặn âm pending
        plat.setPendingBalance(plat.getPendingBalance().subtract(amount).max(java.math.BigDecimal.ZERO));
        plat.setTotalBalance(plat.getTotalBalance().subtract(amount));
        plat.setRefundedTotal(plat.getRefundedTotal().add(amount));
        plat.setUpdatedAt(java.time.LocalDateTime.now());
        platformWalletRepo.save(plat);

        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(order.getId())
                .amount(amount)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .description("Partial refund for storeOrder " + storeOrder.getId())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        platformTxRepo.save(ptx);

        // 2) Gỡ hold của shop tương ứng (NEVER lấy tiền từ shop)
        StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeOrder.getStore().getStoreId())
                .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeOrder.getStore().getStoreId()));

        // CHANGED: chỉ hạ pending, không đụng available; chặn âm
        sw.setPendingBalance(sw.getPendingBalance().subtract(amount).max(java.math.BigDecimal.ZERO));
        sw.setUpdatedAt(java.time.LocalDateTime.now());
        storeWalletRepo.save(sw);

        // CHANGED: dùng PENDING_REVERSED thay vì RELEASE_PENDING
        StoreWalletTransaction stx = StoreWalletTransaction.builder()
                .wallet(sw)
                .type(StoreWalletTransactionType.RELEASE_PENDING)
                .amount(amount)
                .balanceAfter(sw.getAvailableBalance())
                .description("Reverse pending due to cancellation " + storeOrder.getId())
                .orderId(order.getId())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        storeWalletTxRepo.save(stx);

        // 3) Cộng ví khách
        Wallet wallet = walletRepo.findByCustomer_Id(order.getCustomer().getId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));
        var oldBalance = wallet.getBalance();                   // CHANGED
        wallet.setBalance(oldBalance.add(amount));
        wallet.setLastTransactionAt(java.time.LocalDateTime.now());
        walletRepo.save(wallet);

        WalletTransaction wtx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Partial refund for storeOrder " + storeOrder.getId())
                .balanceBefore(oldBalance)                      // CHANGED
                .balanceAfter(oldBalance.add(amount))           // CHANGED
                .orderId(order.getId())
                .build();
        walletTxRepo.save(wtx);
    }

}
