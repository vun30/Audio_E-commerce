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
    private final StoreOrderItemRepository storeOrderItemRepo;

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
                        Collectors.mapping(CustomerOrderItem::getFinalLineTotal,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        for (Map.Entry<UUID, BigDecimal> e : storeTotals.entrySet()) {
            UUID storeId = e.getKey();
            BigDecimal amount = e.getValue();

            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            sw.setPendingBalance(sw.getPendingBalance().add(amount));
            sw.setTotalRevenue(sw.getTotalRevenue().add(amount));
            sw.setUpdatedAt(LocalDateTime.now());
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

        LocalDateTime now = LocalDateTime.now();

        // ===== 1) Lấy toàn bộ item của order =====
        List<StoreOrderItem> allItems = storeOrders.stream()
                .flatMap(so -> Optional.ofNullable(so.getItems()).orElse(List.of()).stream())
                .toList();

        if (allItems.isEmpty()) {
            log.warn("[Settlement] orderId={} không có StoreOrderItem nào", order.getId());
            return;
        }

        // ===== 2) Chỉ chọn item đủ điều kiện trả tiền:
        List<StoreOrderItem> itemsToPayout = allItems.stream()
                .filter(it -> Boolean.TRUE.equals(it.getEligibleForPayout()))
                .filter(it -> !Boolean.TRUE.equals(it.getPayoutProcessed()))
                .toList();

        if (itemsToPayout.isEmpty()) {
            log.info("[Settlement] orderId={} hiện không có item nào eligible_for_payout=true & payoutProcessed=false.", order.getId());
            return;
        }

        log.info("[Settlement] orderId={} có {} item sẽ được payout trong lần này.",
                order.getId(), itemsToPayout.size());

        // Group theo StoreOrder (mỗi shop)
        Map<StoreOrder, List<StoreOrderItem>> itemsByStoreOrder = itemsToPayout.stream()
                .collect(Collectors.groupingBy(StoreOrderItem::getStoreOrder));

        BigDecimal totalProductsAllStores = BigDecimal.ZERO;

        for (Map.Entry<StoreOrder, List<StoreOrderItem>> entry : itemsByStoreOrder.entrySet()) {
            StoreOrder so = entry.getKey();
            List<StoreOrderItem> batchItems = entry.getValue();
            UUID storeId = so.getStore().getStoreId();

            // ===== 3.1 Tổng tiền hàng CỦA BATCH NÀY (chỉ những item eligible=true & payoutProcessed=false) =====
            BigDecimal batchProductsTotal = batchItems.stream()
                    .map(StoreOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (batchProductsTotal.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("[Settlement] storeOrder={} batchProductsTotal<=0 → skip", so.getId());
                continue;
            }

            totalProductsAllStores = totalProductsAllStores.add(batchProductsTotal);

            // ===== 3.2 Tính tỉ lệ của batch so với tổng tiền hàng của cả storeOrder =====
            BigDecimal fullProductsTotal = Optional.ofNullable(so.getItems()).orElse(List.of()).stream()
                    .map(StoreOrderItem::getLineTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (fullProductsTotal.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("[Settlement] storeOrder={} fullProductsTotal<=0 → không thể tính ratio, skip", so.getId());
                continue;
            }

            BigDecimal ratio = batchProductsTotal
                    .divide(fullProductsTotal, 6, RoundingMode.HALF_UP);

            // ===== 3.3 & 3.4: NO DEDUCTIONS — trả 100% tiền hàng =====
            BigDecimal batchExtraShip = BigDecimal.ZERO;
            BigDecimal batchPlatformFeeAmount = BigDecimal.ZERO;
            BigDecimal totalDeductions = BigDecimal.ZERO;

            // ===== 3.5 Net payout cho batch =====
            BigDecimal batchNetPayout = batchProductsTotal.subtract(totalDeductions);
            if (batchNetPayout.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[Settlement] netPayout < 0 → set 0 | storeOrder={} | batchProducts={} | fee={} | extraShip={}",
                        so.getId(), batchProductsTotal, batchPlatformFeeAmount, batchExtraShip);
                batchNetPayout = BigDecimal.ZERO;
            }

            log.info("[Settlement] storeOrder={} | batchProducts={} | ratio={} | batchPlatformFee={} | batchExtraShip={} | batchNet={}",
                    so.getId(), batchProductsTotal, ratio, batchPlatformFeeAmount, batchExtraShip, batchNetPayout);

            // ===== 3.6 Cập nhật StoreOrder (cộng dồn) - note: platformFee/extraShip cộng thêm 0 =====
            BigDecimal oldNetPayout = Optional.ofNullable(so.getNetPayoutToStore()).orElse(BigDecimal.ZERO);
            BigDecimal oldPlatformFee = Optional.ofNullable(so.getPlatformFeeAmount()).orElse(BigDecimal.ZERO);
            BigDecimal oldExtraShip = Optional.ofNullable(so.getShippingExtraForStore()).orElse(BigDecimal.ZERO);

            so.setNetPayoutToStore(oldNetPayout.add(batchNetPayout));
            so.setPlatformFeeAmount(oldPlatformFee.add(batchPlatformFeeAmount)); // sẽ cộng 0
            so.setShippingExtraForStore(oldExtraShip.add(batchExtraShip)); // sẽ cộng 0
            // actualShippingFee giữ nguyên nếu đã có
            // JSON chi tiết (batch + tổng)
            try {
                ObjectNode detail = objectMapper.createObjectNode();
                detail.put("batchProductsTotal", batchProductsTotal.longValueExact());
                detail.put("batchPlatformFeeAmount", batchPlatformFeeAmount.longValueExact());
                detail.put("batchShippingExtraForStore", batchExtraShip.longValueExact());
                detail.put("batchNetPayoutToStore", batchNetPayout.longValueExact());
                // actualShippingFee chỉ put nếu có
                detail.put("netPayoutToStoreTotal", so.getNetPayoutToStore().longValueExact());
                detail.put("platformFeeAmountTotal", so.getPlatformFeeAmount().longValueExact());
                detail.put("shippingExtraForStoreTotal", so.getShippingExtraForStore().longValueExact());
                detail.put("settledAt", now.toString());

                so.setSettlementDetailJson(objectMapper.writeValueAsString(detail));
            } catch (Exception e) {
                log.error("[Settlement] Failed to build settlement_detail_json for storeOrder={}", so.getId(), e);
            }

            storeOrderRepo.save(so);

            // ===== 3.7 Cập nhật ví shop =====
            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            BigDecimal oldPending = Optional.ofNullable(sw.getPendingBalance()).orElse(BigDecimal.ZERO);
            BigDecimal oldAvailable = Optional.ofNullable(sw.getAvailableBalance()).orElse(BigDecimal.ZERO);

            sw.setPendingBalance(oldPending.subtract(batchProductsTotal).max(BigDecimal.ZERO));
            sw.setAvailableBalance(oldAvailable.add(batchNetPayout));
            sw.setUpdatedAt(now);
            storeWalletRepo.save(sw);

            storeWalletTxRepo.save(StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.RELEASE_PENDING)
                    .amount(batchNetPayout)
                    .balanceAfter(sw.getAvailableBalance())
                    .orderId(order.getId())
                    .description(String.format(
                            "Release after hold (partial) | storeOrder=%s | items=%d | net=%s",
                            so.getId(), batchItems.size(), batchNetPayout))
                    .createdAt(now)
                    .build());

            // ===== 3.8 Giao dịch Platform: chỉ tạo PAYOUT_STORE (không có PLATFORM_FEE / SHIPPING_FEE_ADJUST) =====
            platformTxRepo.save(PlatformTransaction.builder()
                    .wallet(plat)
                    .orderId(order.getId())
                    .storeId(storeId)
                    .amount(batchNetPayout)
                    .type(TransactionType.PAYOUT_STORE)
                    .status(TransactionStatus.DONE)
                    .description("Payout to store (partial, full product amount) | storeOrder=" + so.getId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            // ===== 3.9 Ghi doanh thu cho batch =====
            // Lưu doanh thu cửa hàng (platformFee = 0, shippingDiff = 0)
            revenueService.recordStoreRevenue(
                    storeId,
                    so.getId(),
                    batchNetPayout,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    now.toLocalDate()
            );

            // Không record platform revenue vì không có commission / shipping diff

            // ===== 3.10 Đánh dấu các item trong batch đã được xử lý payout =====
            for (StoreOrderItem it : batchItems) {
                it.setPayoutProcessed(true);
            }
            storeOrderItemRepo.saveAll(batchItems);
        }

        // ===== 4) Cập nhật PlatformWallet cho phần đã payout trong lần này =====
        BigDecimal oldPendingPlat = Optional.ofNullable(plat.getPendingBalance()).orElse(BigDecimal.ZERO);
        BigDecimal oldDonePlat = Optional.ofNullable(plat.getDoneBalance()).orElse(BigDecimal.ZERO);

        plat.setPendingBalance(oldPendingPlat.subtract(totalProductsAllStores).max(BigDecimal.ZERO));
        plat.setDoneBalance(oldDonePlat.add(totalProductsAllStores));
        plat.setUpdatedAt(now);
        platformWalletRepo.save(plat);

        // ===== 5) Finalize HOLD tx nếu không còn item pending payout =====
        boolean hasRemainingItemToPayout = allItems.stream()
                .anyMatch(it ->
                        Boolean.TRUE.equals(it.getEligibleForPayout())
                                && !Boolean.TRUE.equals(it.getIsPayout())
                );

        var pendingHoldTxs = platformTxRepo.findAllByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING)
                .stream()
                .filter(tx -> tx.getType() == TransactionType.HOLD)
                .toList();

        if (!hasRemainingItemToPayout) {
            pendingHoldTxs.forEach(tx -> {
                tx.setStatus(TransactionStatus.DONE);
                tx.setUpdatedAt(now);
            });
            log.info("[Settlement] orderId={} đã payout xong tất cả item eligible_for_payout=true. HOLD tx chuyển DONE.", order.getId());
        } else {
            log.info("[Settlement] orderId={} vẫn còn item eligible_for_payout=true & is_payout=false → giữ HOLD PENDING.",
                    order.getId());
        }

        log.info("[Settlement] SUCCESS (partial) | orderId={} | totalProductsPayout={}",
                order.getId(), totalProductsAllStores);
    }




    @Transactional
    public void refundEntireOrderToCustomerWallet(CustomerOrder order) {
        if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
            log.info("[Settlement] Skip refundEntireOrderToCustomerWallet – paymentMethod={}", order.getPaymentMethod());
            return;
        }
        // ===== 0) Tính các khoản =====
        BigDecimal productsTotal = order.getItems().stream()
                .map(CustomerOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingTotal = Optional
                .ofNullable(order.getShippingFeeTotal())
                .orElse(BigDecimal.ZERO);

        BigDecimal discountTotal = Optional
                .ofNullable(order.getDiscountTotal())
                .orElse(BigDecimal.ZERO);

        // KH thực tế đã trả cho đơn này (đã có ship + trừ voucher)
        BigDecimal refundAmount = productsTotal
                .add(shippingTotal)
                .subtract(discountTotal);

        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        // 1) Platform (refund từ platform → customer)
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // Giả định: HOLD chỉ chứa tiền hàng (productsTotal)
        // → hạ pending theo productsTotal, nhưng totalBalance/refundedTotal theo refundAmount (grand total)
        BigDecimal oldPending = plat.getPendingBalance();
        BigDecimal oldTotal = plat.getTotalBalance();
        BigDecimal oldRefunded = plat.getRefundedTotal();

        plat.setPendingBalance(oldPending.subtract(productsTotal).max(java.math.BigDecimal.ZERO));
        plat.setTotalBalance(oldTotal.subtract(refundAmount));
        plat.setRefundedTotal(oldRefunded.add(refundAmount));
        plat.setUpdatedAt(java.time.LocalDateTime.now());
        platformWalletRepo.save(plat);

        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(order.getId())
                .amount(refundAmount) // ✅ ghi nhận refund đúng số KH được hoàn (grand total)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .description("Refund entire order (grand total) to customer")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        platformTxRepo.save(ptx);

        // 2) Gỡ pending từng shop (nếu đã allocate) – chỉ gỡ phần tiền hàng
        var storeTotals = order.getItems().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CustomerOrderItem::getStoreId,
                        java.util.stream.Collectors.mapping(CustomerOrderItem::getLineTotal,
                                java.util.stream.Collectors.reducing(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                ));

        for (var e : storeTotals.entrySet()) {
            UUID storeId = e.getKey();
            BigDecimal amountProducts = e.getValue(); // chỉ tiền hàng của store đó

            StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeId)
                    .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeId));

            BigDecimal oldStorePending = sw.getPendingBalance();

            // chỉ hạ pending, không đụng available; chặn âm
            sw.setPendingBalance(oldStorePending.subtract(amountProducts).max(BigDecimal.ZERO));
            sw.setUpdatedAt(LocalDateTime.now());
            storeWalletRepo.save(sw);

            StoreWalletTransaction stx = StoreWalletTransaction.builder()
                    .wallet(sw)
                    .type(StoreWalletTransactionType.RELEASE_PENDING) // hoặc PENDING_REVERSED nếu bạn có enum riêng
                    .amount(amountProducts) // ✅ chỉ log phần pending bị reverse
                    .balanceAfter(sw.getAvailableBalance())
                    .description("Reverse pending (products) due to full order cancel " + order.getId())
                    .orderId(order.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            storeWalletTxRepo.save(stx);
        }

        // 3) Cộng ví khách = refundAmount (grand total)
        Wallet wallet = walletRepo.findByCustomer_Id(order.getCustomer().getId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));
        var oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.add(refundAmount));
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        WalletTransaction wtx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(refundAmount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Refund (grand total) for order " + order.getId())
                .balanceBefore(oldBalance)
                .balanceAfter(oldBalance.add(refundAmount))
                .orderId(order.getId())
                .build();
        walletTxRepo.save(wtx);
    }



    /** Refund một PHẦN theo storeOrder (KH đã thanh toán online, đơn đang AWAITING_SHIPMENT, shop duyệt). */
    @Transactional
    public void refundStorePartToCustomerWallet(StoreOrder storeOrder) {
        CustomerOrder order = storeOrder.getCustomerOrder();
        if (order == null || order.getPaymentMethod() != PaymentMethod.ONLINE) {
            log.info("[Settlement] Skip refundStorePartToCustomerWallet – paymentMethod={}",
                    order != null ? order.getPaymentMethod() : null);
            return;
        }
        // ===== 0) Tính các khoản theo store =====
        BigDecimal productsTotal = storeOrder.getItems().stream()
                .map(StoreOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal customerShipFee = Optional
                .ofNullable(storeOrder.getShippingFee())
                .orElse(BigDecimal.ZERO);

        BigDecimal storeDiscount = Optional
                .ofNullable(storeOrder.getStoreVoucherDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal platformDiscount = Optional
                .ofNullable(storeOrder.getPlatformVoucherDiscount())
                .orElse(BigDecimal.ZERO);

        // Số tiền KH thực trả cho store này:
        // sản phẩm + phí ship - (giảm giá shop + giảm giá platform)
        BigDecimal refundAmount = productsTotal
                .add(customerShipFee)
                .subtract(storeDiscount)
                .subtract(platformDiscount);

        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        // 1) Trả từ Platform → Customer
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        BigDecimal oldPending = plat.getPendingBalance();
        BigDecimal oldTotal = plat.getTotalBalance();
        BigDecimal oldRefunded = plat.getRefundedTotal();

        // Giả định HOLD chỉ giữ productsTotal
        plat.setPendingBalance(oldPending.subtract(productsTotal).max(BigDecimal.ZERO));
        plat.setTotalBalance(oldTotal.subtract(refundAmount));
        plat.setRefundedTotal(oldRefunded.add(refundAmount));
        plat.setUpdatedAt(LocalDateTime.now());
        platformWalletRepo.save(plat);

        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(order.getId())
                .amount(refundAmount) // ✅ đúng số tiền hoàn lại cho KH
                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .description("Partial refund (grand total share) for storeOrder " + storeOrder.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        platformTxRepo.save(ptx);

        // 2) Gỡ hold của shop tương ứng – chỉ gỡ phần tiền hàng
        StoreWallet sw = storeWalletRepo.findByStore_StoreId(storeOrder.getStore().getStoreId())
                .orElseThrow(() -> new NoSuchElementException("Store wallet not found: " + storeOrder.getStore().getStoreId()));

        BigDecimal oldStorePending = sw.getPendingBalance();
        sw.setPendingBalance(oldStorePending.subtract(productsTotal).max(BigDecimal.ZERO));
        sw.setUpdatedAt(LocalDateTime.now());
        storeWalletRepo.save(sw);

        StoreWalletTransaction stx = StoreWalletTransaction.builder()
                .wallet(sw)
                .type(StoreWalletTransactionType.RELEASE_PENDING) // hoặc PENDING_REVERSED nếu bạn tách enum
                .amount(productsTotal) // ✅ chỉ phần pending bị reverse
                .balanceAfter(sw.getAvailableBalance())
                .description("Reverse pending (products) due to cancellation " + storeOrder.getId())
                .orderId(order.getId())
                .createdAt(LocalDateTime.now())
                .build();
        storeWalletTxRepo.save(stx);

        // 3) Cộng ví khách = refundAmount (share của store này)
        Wallet wallet = walletRepo.findByCustomer_Id(order.getCustomer().getId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.add(refundAmount));
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(wallet);

        WalletTransaction wtx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(refundAmount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Partial refund (grand total share) for storeOrder " + storeOrder.getId())
                .balanceBefore(oldBalance)
                .balanceAfter(oldBalance.add(refundAmount))
                .orderId(order.getId())
                .build();
        walletTxRepo.save(wtx);
    }

    @Transactional
    public void recordCodDeliverySuccess(CustomerOrder order) {
        // Chỉ xử lý COD
        if (order == null || order.getPaymentMethod() != PaymentMethod.COD) {
            log.info("[Settlement] Skip recordCodDeliverySuccess – paymentMethod={}",
                    order != null ? order.getPaymentMethod() : null);
            return;
        }

        // Nếu đã có HOLD PENDING cho order này rồi thì bỏ (idempotent)
        var existingPending = platformTxRepo.findAllByOrderIdAndStatus(
                order.getId(), TransactionStatus.PENDING
        );
        boolean alreadyHasHold = existingPending.stream()
                .anyMatch(tx -> tx.getType() == TransactionType.HOLD);
        if (alreadyHasHold) {
            log.info("[Settlement] COD HOLD already exists for order {} → skip", order.getId());
            return;
        }

        // ===== 1) Tính tổng tiền hàng (KH đã trả cho shipper) =====
        BigDecimal productsTotal = order.getItems().stream()
                .map(CustomerOrderItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (productsTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[Settlement] recordCodDeliverySuccess order={} productsTotal<=0 → skip", order.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // ===== 2) Đẩy tiền vào ví PLATFORM (pending) + log HOLD =====
        PlatformWallet plat = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        plat.setTotalBalance(plat.getTotalBalance().add(productsTotal));
        plat.setPendingBalance(plat.getPendingBalance().add(productsTotal));
        plat.setReceivedTotal(plat.getReceivedTotal().add(productsTotal));
        plat.setUpdatedAt(now);
        platformWalletRepo.save(plat);

        PlatformTransaction holdTx = PlatformTransaction.builder()
                .wallet(plat)
                .orderId(order.getId())
                .amount(productsTotal)
                .type(TransactionType.HOLD)
                .status(TransactionStatus.PENDING)
                .description("COD collected – holding 7 days")
                .createdAt(now)
                .updatedAt(now)
                .build();
        platformTxRepo.save(holdTx);

        // ===== 3) Allocate sang pending của từng store (giống online) =====
        allocateToStoresPending(order);

        log.info("[Settlement] recordCodDeliverySuccess DONE | orderId={} | productsTotal={}",
                order.getId(), productsTotal);
    }

}
