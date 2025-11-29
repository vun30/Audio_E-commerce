package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.CustomerOrderRepository;
import org.example.audio_ecommerce.repository.NotificationRepository;
import org.example.audio_ecommerce.repository.PlatformTransactionRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.Impl.SettlementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletReleaseScheduler {

    private final PlatformTransactionRepository platformTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final SettlementService settlementService;
    private final NotificationRepository notificationRepo;
    private final StoreOrderRepository storeOrderRepository;

    //Chạy mỗi ngày lúc 01:00 sáng (prod)
      //Cron test mỗi phút: "0 */1 * * * ?" (đang dùng để debug)

    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void releaseHeldFunds() {
        // Ngày cutoff: đơn đã DELIVERY_SUCCESS trước thời điểm này mới được release
        // PROD: 7 ngày
        LocalDateTime deliveredCutoff = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        // TEST: 1 phút
        // LocalDateTime deliveredCutoff = LocalDateTime.now().minusMinutes(1);

        log.info("🔍 [Scheduler] Quét release các giao dịch HOLDING với DELIVERY_SUCCESS trước {}", deliveredCutoff);

        // Lấy toàn bộ HOLD còn đang PENDING
        List<PlatformTransaction> holdingTxs =
                platformTransactionRepository.findAllByTypeAndStatus(
                        TransactionType.HOLD,
                        TransactionStatus.PENDING
                );

        if (holdingTxs.isEmpty()) {
            log.info("✅ Không có giao dịch HOLD PENDING nào.");
            return;
        }

        int processed = 0;
        for (PlatformTransaction tx : holdingTxs) {
            if (tx.getOrderId() == null) {
                log.warn("⚠ Bỏ qua HOLD tx={} vì orderId = null", tx.getId());
                continue;
            }

            CustomerOrder order = customerOrderRepository.findById(tx.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("⚠ Bỏ qua HOLD tx={} vì không tìm thấy CustomerOrder {}", tx.getId(), tx.getOrderId());
                continue;
            }

            // Nếu đơn đã hủy / chưa thanh toán thì không release
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.UNPAID) {
                log.info("⏭ Bỏ qua order={} (status={})", order.getId(), order.getStatus());
                continue;
            }

            // Chỉ release khi đơn đã DELIVERY_SUCCESS
            if (order.getStatus() != OrderStatus.DELIVERY_SUCCESS) {
                log.info("⏭ Order {} chưa DELIVERY_SUCCESS (status={}) → chưa release", order.getId(), order.getStatus());
                continue;
            }

            // Lấy thời điểm giao thành công – bạn thay bằng field đúng của mình
            LocalDateTime deliveredAt = order.getDeliveredAt(); // giả định có field này
            if (deliveredAt == null) {
                log.warn("⚠ Order {} status=DELIVERY_SUCCESS nhưng deliveredAt=null → chưa đủ điều kiện release", order.getId());
                continue;
            }

            // Kiểm tra đã đủ 7 ngày từ khi DELIVERY_SUCCESS chưa
            if (deliveredAt.isAfter(deliveredCutoff)) {
                log.info("⏭ Order {} mới DELIVERY_SUCCESS lúc {} < cutoff {} → đợi thêm",
                        order.getId(), deliveredAt, deliveredCutoff);
                continue;
            }

            // Đến đây: đủ điều kiện release
            try {
                settlementService.releaseAfterHold(order);
                processed++;

                log.info("💸 Released orderId={} amount={} (txId={})",
                        order.getId(), tx.getAmount(), tx.getId());
            } catch (Exception e) {
                log.error("❌ Release thất bại orderId={} txId={}: {}",
                        order.getId(), tx.getId(), e.getMessage(), e);
            }
        }

        log.info("🏁 [Scheduler] Hoàn tất quét release. Đã xử lý {} giao dịch.", processed);
    }

    private void notifyReleaseSuccess(CustomerOrder order) {
        try {
            // Lấy storeOrder đầu tiên của CustomerOrder
            StoreOrder so = storeOrderRepository.findAllByCustomerOrder_Id(order.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (so == null) {
                log.warn("Không tìm thấy StoreOrder cho CustomerOrder {}", order.getId());
                return;
            }

            Store store = so.getStore();

            // ===== Notify cho STORE =====
            notificationRepo.save(Notification.builder()
                    .target(NotificationTarget.STORE)
                    .targetId(store.getStoreId())
                    .type(NotificationType.WALLET_RELEASE) // nhớ có enum này
                    .title("Tiền đã được giải phóng")
                    .message("Đơn hàng " + order.getOrderCode() +
                            " đã qua thời gian giữ tiền, số tiền tạm giữ đã được chuyển vào ví cửa hàng.")
                    .actionUrl("/seller/orders/" + so.getId()) // hoặc customerOrderId tuỳ FE
                    .read(false)
                    .build()
            );

            // ===== Notify cho CUSTOMER =====
            notificationRepo.save(Notification.builder()
                    .target(NotificationTarget.CUSTOMER)
                    .targetId(order.getCustomer().getId())
                    .type(NotificationType.WALLET_RELEASE)
                    .title("Đơn hàng đã hoàn tất")
                    .message("Đơn hàng " + order.getOrderCode() +
                            " đã hoàn tất, tiền giữ trên hệ thống đã được xử lý.")
                    .actionUrl("/customer/orders/" + order.getId())
                    .read(false)
                    .build()
            );

        } catch (Exception e) {
            log.error("❌ Lỗi tạo notification release cho order {}: {}",
                    order.getId(), e.getMessage(), e);
        }
    }


}
