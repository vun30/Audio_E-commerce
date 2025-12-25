package org.example.audio_ecommerce.service.Impl;


import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;

import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;

import org.example.audio_ecommerce.service.AutoCancelOrderService;
import org.example.audio_ecommerce.service.LegalPointService;
import org.example.audio_ecommerce.service.NotificationCreatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutoCancelOrderServiceImpl implements AutoCancelOrderService {

    private final StoreOrderRepository storeOrderRepo;
    private final CustomerOrderRepository customerOrderRepo;

    private final LegalPointService legalPointService; // bạn đang có dạng này
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxnRepo;

    private final PlatformWalletRepository platformWalletRepo;
    private final PlatformTransactionRepository platformTxnRepo;

    private final NotificationCreatorService notificationCreatorService;

    @Transactional
    @Override
    public void autoCancelWholeCustomerOrder(StoreOrder triggerStoreOrder, String reasonCode, String message) {

        // reload cho chắc
        StoreOrder trigger = storeOrderRepo.findById(triggerStoreOrder.getId())
                .orElseThrow();

        // chống chạy trùng
        if (Boolean.TRUE.equals(trigger.getAutoCancelApplied())) return;

        CustomerOrder co = trigger.getCustomerOrder(); // StoreOrder có customerOrder :contentReference[oaicite:2]{index=2}

        // nếu CustomerOrder đã cancel/completed thì skip
        if (co.getStatus() == OrderStatus.CANCELLED || co.getStatus() == OrderStatus.COMPLETED) {
            return;
        }

        // 1) Cancel tất cả storeOrders thuộc customerOrder
        var allStoreOrders = storeOrderRepo.findAllByCustomerOrder_Id(co.getId());

        for (StoreOrder so : allStoreOrders) {
            if (so.getStatus() == OrderStatus.CANCELLED || so.getStatus() == OrderStatus.COMPLETED) continue;

            so.setStatus(OrderStatus.CANCELLED);
            so.setAutoCancelApplied(true);
            // nếu bạn có message riêng cho store-order thì set; không có thì thôi
            storeOrderRepo.save(so);

            // 2) phạt uy tín store (mỗi storeOrder bị cancel phạt 1 lần)
            // tránh trừ lặp: nếu bạn có flag khác thì dùng; tạm dùng autoCancelApplied là đủ cho cron
            legalPointService.minusForStore(so.getStore().getStoreId(), 1);
            notificationCreatorService.createAndSend(
                    NotificationTarget.STORE,
                    so.getStore().getStoreId(),
                    NotificationType.ORDER_CANCELLED,
                    "Đơn hàng bị huỷ do quá thời gian xử lý",
                    message,
                    "/store/orders/" + so.getId(),
                    """
                    {
                      "storeOrderId": "%s",
                      "customerOrderId": "%s",
                      "reasonCode": "%s",
                      "autoCancel": true
                    }
                    """.formatted(
                            so.getId(),
                            co.getId(),
                            reasonCode
                    ),
                    Map.of(
                            "storeOrderId", so.getId().toString(),
                            "type", "AUTO_CANCEL"
                    )
            );

        }

        // 3) Cancel customerOrder + set message để FE hiển thị (CustomerOrder có field message :contentReference[oaicite:3]{index=3})
        co.setStatus(OrderStatus.CANCELLED);
        co.setMessage(message);
        customerOrderRepo.save(co);

        // 4) nếu online (PayOS) thì refund: platform cashBalance -> customer wallet
        if (co.getPaymentMethod() == PaymentMethod.ONLINE) { // đổi đúng enum bạn đặt
            refundOnlineFromPlatformToCustomer(co, reasonCode, message);
        }

        notificationCreatorService.createAndSend(
                NotificationTarget.CUSTOMER,
                co.getCustomer().getId(),
                NotificationType.ORDER_CANCELLED,
                "Đơn hàng đã bị huỷ",
                message,
                "/orders/" + co.getId(), // FE deeplink
                """
                {
                  "orderId": "%s",
                  "reasonCode": "%s",
                  "autoCancel": true
                }
                """.formatted(co.getId(), reasonCode),
                Map.of(
                        "orderId", co.getId().toString(),
                        "type", "AUTO_CANCEL"
                )
        );

    }

    private void refundOnlineFromPlatformToCustomer(CustomerOrder co, String reasonCode, String message) {

        BigDecimal refundAmount = co.getGrandTotal() != null ? co.getGrandTotal() : BigDecimal.ZERO; // CustomerOrder có grandTotal :contentReference[oaicite:4]{index=4}
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        // idempotency key chống hoàn tiền lặp
        String idem = "AUTO_CANCEL_REFUND:" + co.getId();

        boolean existed = platformTxnRepo.existsByIdempotencyKey(idem);
        if (existed) return;

        // 4.1) lấy platform wallet (ownerType=PLATFORM)
        PlatformWallet pw = platformWalletRepo.findFirstByOwnerType(WalletOwnerType.PLATFORM)
                .orElseThrow(() -> new IllegalStateException("Platform wallet not found"));

        if (pw.getCashBalance().compareTo(refundAmount) < 0) {
            throw new IllegalStateException("Platform cashBalance not enough to refund");
        }

        BigDecimal pwBefore = pw.getCashBalance();
        BigDecimal pwAfter = pwBefore.subtract(refundAmount);

        pw.setCashBalance(pwAfter);
        pw.setUpdatedAt(LocalDateTime.now());
        platformWalletRepo.save(pw);

        // 4.2) ghi platform transaction (OUT)
        PlatformTransaction ptx = PlatformTransaction.builder()
                .wallet(pw)
                .orderId(co.getId())
                .customerId(co.getCustomer().getId())
                .amount(refundAmount)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.DONE)
                .channel(PaymentChannel.PAYOS)
                .bucket(WalletBucket.CASH)
                .direction(TxDirection.OUT)
                .balanceBefore(pwBefore)
                .balanceAfter(pwAfter)
                .idempotencyKey(idem)
                .description("[AUTO_CANCEL] Refund online order: " + reasonCode)
                .metadataJson("{\"message\":\"" + safeJson(message) + "\"}")
                .build();
        platformTxnRepo.save(ptx);

        // 4.3) cộng tiền về customer wallet + wallet transaction
        Wallet w = walletRepo.findByCustomer_Id(co.getCustomer().getId())
                .orElseThrow(() -> new IllegalStateException("Customer wallet not found"));

        BigDecimal wBefore = w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO;
        BigDecimal wAfter = wBefore.add(refundAmount);

        w.setBalance(wAfter);
        w.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(w);

        WalletTransaction wtx = WalletTransaction.builder()
                .wallet(w)
                .amount(refundAmount)
                .transactionType(WalletTransactionType.REFUND)
                .status(WalletTransactionStatus.SUCCESS)
                .description("[AUTO_CANCEL] Hoàn tiền đơn " + co.getOrderCode())
                .balanceBefore(wBefore)
                .balanceAfter(wAfter)
                .orderId(co.getId())
                .externalRef(idem)
                .build();
        walletTxnRepo.save(wtx);
    }

    private String safeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

