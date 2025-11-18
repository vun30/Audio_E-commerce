package org.example.audio_ecommerce.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.RevenueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private final PlatformWalletRepository platformWalletRepo;
    private final PlatformTransactionRepository platformTxRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final StoreWalletRepository storeWalletRepo;
    private final StoreWalletTransactionRepository storeWalletTxRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final GhnOrderRepository ghnOrderRepo;
    private final PlatformFeeRepository platformFeeRepo;
    private final RevenueService revenueService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        List<StoreOrder> storeOrders = storeOrderRepo.findAllByCustomerOrder_Id(order.getId());
        if (storeOrders == null || storeOrders.isEmpty()) {
            throw new IllegalStateException("No StoreOrders for CustomerOrder " + order.getId());
        }

        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        BigDecimal platformFeeRate = getCurrentPlatformFeeRate();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalProductsAllStores = BigDecimal.ZERO;
        BigDecimal totalNetPayoutAllStores = BigDecimal.ZERO;

        for (StoreOrder so : storeOrders) {
            UUID storeId = so.getStore().getStoreId();

            // 2.1 Tổng tiền sản phẩm
            BigDecimal productsTotal = so.getItems().stream()
                    .map(StoreOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalProductsAllStores = totalProductsAllStores.add(productsTotal);

            // 2.2 Phí vận chuyển
            BigDecimal actualShipFee = ghnOrderRepo.findByStoreOrderId(so.getId())
                    .map(GhnOrder::getTotalFee)
                    .orElse(BigDecimal.ZERO);
            BigDecimal customerShipFee = Optional.ofNullable(so.getShippingFee()).orElse(BigDecimal.ZERO);
            BigDecimal extraShip = actualShipFee.subtract(customerShipFee);
            if (extraShip.compareTo(BigDecimal.ZERO) < 0) {
                extraShip = BigDecimal.ZERO;
            }

            // 2.3 Phí nền tảng
            BigDecimal platformFeeAmount = productsTotal.multiply(platformFeeRate)
                    .setScale(0, RoundingMode.DOWN);

            BigDecimal totalDeductions = extraShip.add(platformFeeAmount);

            // 2.4 Net payout
            BigDecimal netPayout = productsTotal.subtract(totalDeductions);
            if (netPayout.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[Settlement] netPayout < 0 → set to 0 | storeOrder={} | products={} | deductions={} | extraShip={} | platformFee={}",
                        so.getId(), productsTotal, totalDeductions, extraShip, platformFeeAmount);
                netPayout = BigDecimal.ZERO;
            }
            totalNetPayoutAllStores = totalNetPayoutAllStores.add(netPayout);

            // GHI CHI TIẾT VÀO StoreOrder (rất quan trọng cho FE và đối soát)
            so.setActualShippingFee(actualShipFee);
            so.setShippingExtraForStore(extraShip);
            so.setPlatformFeeAmount(platformFeeAmount);
            so.setNetPayoutToStore(netPayout);

            // Tạo JSON chi tiết để FE hiển thị đẹp
            try {
                ObjectNode detail = objectMapper.createObjectNode();
                detail.put("productsTotal", productsTotal.longValueExact());
                detail.put("customerShippingFee", customerShipFee.longValueExact());
                detail.put("actualShippingFee", actualShipFee.longValueExact());
                detail.put("shippingExtraForStore", extraShip.longValueExact());
                detail.put("platformFeeRate", platformFeeRate.stripTrailingZeros().toPlainString());
                detail.put("platformFeeAmount", platformFeeAmount.longValueExact());
                detail.put("netPayoutToStore", netPayout.longValueExact());
                detail.put("settledAt", now.toString());

                so.setSettlementDetailJson(objectMapper.writeValueAsString(detail));
            } catch (Exception e) {
                log.error("[Settlement] Failed to build settlement_detail_json for storeOrder={}", so.getId(), e);
                so.setSettlementDetailJson(null); // hoặc fallback string đơn giản
            }

            // Lưu StoreOrder trước khi cập nhật ví (vì có thể cần đọc lại)
            storeOrderRepo.save(so);

            // 2.5 Cập nhật StoreWallet
            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            BigDecimal oldPending = Optional.ofNullable(sw.getPendingBalance()).orElse(BigDecimal.ZERO);
            BigDecimal oldAvailable = Optional.ofNullable(sw.getAvailableBalance()).orElse(BigDecimal.ZERO);

            sw.setPendingBalance(oldPending.subtract(productsTotal).max(BigDecimal.ZERO));
            sw.setAvailableBalance(oldAvailable.add(netPayout));
            sw.setUpdatedAt(now);
            storeWalletRepo.save(sw);

            // 2.6 Giao dịch ví shop
            storeWalletTxRepo.save(StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.RELEASE_PENDING)
                    .amount(netPayout)
                    .balanceAfter(sw.getAvailableBalance())
                    .orderId(order.getId())
                    .description(String.format(
                            "Release after hold | storeOrder=%s | net=%s", so.getId(), netPayout))
                    .createdAt(now)
                    .build());

            // 2.7 Payout cho shop
            platformTxRepo.save(PlatformTransaction.builder()
                    .wallet(plat)
                    .orderId(order.getId())
                    .storeId(storeId)
                    .amount(netPayout)
                    .type(TransactionType.PAYOUT_STORE)
                    .status(TransactionStatus.DONE)
                    .description("Payout to store | storeOrder=" + so.getId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            // 2.8 Phí nền tảng
            if (platformFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
                platformTxRepo.save(PlatformTransaction.builder()
                        .wallet(plat)
                        .orderId(order.getId())
                        .storeId(storeId)
                        .amount(platformFeeAmount)
                        .type(TransactionType.PLATFORM_FEE)
                        .status(TransactionStatus.DONE)
                        .description("Platform fee | storeOrder=" + so.getId())
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            // 2.9 Phí ship dôi
            if (extraShip.compareTo(BigDecimal.ZERO) > 0) {
                platformTxRepo.save(PlatformTransaction.builder()
                        .wallet(plat)
                        .orderId(order.getId())
                        .storeId(storeId)
                        .amount(extraShip)
                        .type(TransactionType.SHIPPING_FEE_ADJUST)
                        .status(TransactionStatus.DONE)
                        .description("Extra shipping fee charged to store | storeOrder=" + so.getId())
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            // 🔟 GHI DOANH THU (SHOP + NỀN TẢNG)
            // Doanh thu shop (net nhận được sau khi trừ phí)
            revenueService.recordStoreRevenue(
                    storeId,
                    so.getId(),
                    netPayout,
                    platformFeeAmount,
                    extraShip,
                    now.toLocalDate()
            );

            // Doanh thu nền tảng - phí hoa hồng
            if (platformFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
                revenueService.recordPlatformRevenue(
                        so.getId(),
                        PlatformRevenueType.COMMISSION,
                        platformFeeAmount,
                        now.toLocalDate()
                );
            }

            // Doanh thu nền tảng - chênh lệch ship
            if (extraShip.compareTo(BigDecimal.ZERO) > 0) {
                revenueService.recordPlatformRevenue(
                        so.getId(),
                        PlatformRevenueType.SHIPPING_DIFF,
                        extraShip,
                        now.toLocalDate()
                );
            }
        }

        // 3. Cập nhật PlatformWallet tổng
        plat.setPendingBalance(plat.getPendingBalance().subtract(totalProductsAllStores).max(BigDecimal.ZERO));
        plat.setDoneBalance(
                Optional.ofNullable(plat.getDoneBalance()).orElse(BigDecimal.ZERO).add(totalProductsAllStores));
        plat.setUpdatedAt(now);
        platformWalletRepo.save(plat);

        // 4. Đánh dấu HOLD → DONE
        platformTxRepo.findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING)
                .forEach(tx -> {
                    tx.setStatus(TransactionStatus.DONE);
                    tx.setUpdatedAt(now);
                });
        // Nếu repo hỗ trợ saveAll thì tốt hơn
        // platformTxRepo.saveAll(pendingTxs);

        log.info("[Settlement] SUCCESS | orderId={} | totalProducts={} | totalPayoutToStores={} | stores={}",
                order.getId(), totalProductsAllStores, totalNetPayoutAllStores, storeOrders.size());
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

    private BigDecimal getCurrentPlatformFeeRate() {
        return platformFeeRepo.findFirstByIsActiveTrueOrderByEffectiveDateDesc()
                .map(f -> f.getPercentage()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)) // 5.00 → 0.0500
                .orElse(BigDecimal.ZERO);
    }


}
