package org.example.audio_ecommerce.scheduler;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.email.dto.StoreStatusChangedData;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreDebtUnlockService {

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final TaskScheduler taskScheduler; // giữ lại (để tương thích), nhưng không bắt buộc dùng nữa

    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");
    private static final BigDecimal SAFE_DEPOSIT_RATIO = new BigDecimal("0.10"); // 10%   cọc hơn nợ 10%

    @Value("${app.site-url:}")
    private String siteUrl;

    /**
     * ✅ AUTO: Tự quét các store đang SUSPENDED_DEBT và thử mở khóa.
     * Chỉ mở khi đủ điều kiện. Không đổi trạng thái => không gửi mail.
     */
    @Scheduled(cron = "0 */1 * * * *") // mỗi 2 phút
    @Transactional
    public void autoUnlockDebtStores() {

        List<Store> lockedStores = storeRepository.findByStatus(StoreStatus.SUSPENDED_DEBT);
        if (lockedStores == null || lockedStores.isEmpty()) return;

        int scanned = 0;
        int unlocked = 0;

        for (Store s : lockedStores) {
            if (s == null || s.getStoreId() == null) continue;
            scanned++;

            boolean ok = tryUnlockStoreInternal(s.getStoreId());
            if (ok) unlocked++;
        }

        log.info("[AUTO-UNLOCK] scanned={} unlocked={}", scanned, unlocked);
    }

    /**
     * ❌ (Không cần nữa nếu đã dùng auto cron)
     * Giữ lại theo yêu cầu "comment không xóa"
     */
    /*
    public void scheduleUnlockCheck(UUID storeId) {
        taskScheduler.schedule(
                () -> tryUnlockStore(storeId),
                Instant.now().plusSeconds(180) // ⏱️ delay 3 phút
        );
    }
    */

    /**
     * (Optional) Nếu chỗ khác vẫn muốn gọi manual, vẫn cho public method.
     * Method này sẽ chạy trong transaction nếu được gọi từ bên ngoài bean.
     */
    @Transactional
    public boolean tryUnlockStore(UUID storeId) {
        return tryUnlockStoreInternal(storeId);
    }

    /**
     * Core logic - trả về true nếu có mở khóa thực sự.
     */
    private boolean tryUnlockStoreInternal(UUID storeId) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return false;

        // ❌ Bỏ qua store bị suspend vĩnh viễn / admin
        StoreStatus currentStatus = store.getStatus();
        if (currentStatus == StoreStatus.SUSPENDED
                || currentStatus == StoreStatus.ABANDONED
                || currentStatus == StoreStatus.REJECTED) {

            log.info("[UNLOCK-SKIP][PERMANENT] storeId={} status={}", storeId, currentStatus);
            return false;
        }

        // ✅ Chỉ mở khóa nếu bị khóa do nợ
        if (currentStatus != StoreStatus.SUSPENDED_DEBT) return false;

        // ✅ Load wallet bằng repository để tránh LazyInitialization
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId).orElse(null);
        if (wallet == null) {
            log.warn("[UNLOCK-SKIP] storeId={} wallet not found", storeId);
            return false;
        }

        BigDecimal debt = nz(wallet.getDebtBalance());
        BigDecimal deposit = nz(wallet.getDepositBalance());
        BigDecimal legalPoint = nz(store.getLegalPoint());

        BigDecimal adjustedLimit = deposit.add(legalPoint.multiply(LEGAL_BONUS_UNIT));

        boolean passDebtLimit = debt.compareTo(adjustedLimit) < 0;
        boolean passDepositSafe = deposit.compareTo(debt.multiply(SAFE_DEPOSIT_RATIO)) >= 0;

        if (!passDebtLimit || !passDepositSafe) {
            log.info("[UNLOCK-SKIP] storeId={} debt={} deposit={} limit={}",
                    storeId, debt, deposit, adjustedLimit);
            return false;
        }

        // ===== MỞ KHÓA STORE =====
        store.setStatus(StoreStatus.ACTIVE);
        store.setLastRiskWarningAt(LocalDateTime.now());
        storeRepository.save(store);

        // ===== KHÔI PHỤC PRODUCT =====
        productRepository.bulkUpdateStatusByStoreAndStatus(
                storeId,
                ProductStatus.SUSPENDED_DEBT,
                ProductStatus.ACTIVE
        );

        productRepository.bulkUpdateStatusByStoreAndStatus(
                storeId,
                ProductStatus.UNLISTED_BEFORE_SUSPENDED_DEBT,
                ProductStatus.UNLISTED
        );

        // ===== GỬI MAIL CHỈ KHI THỰC SỰ ĐỔI TRẠNG THÁI =====
        Account acc = store.getAccount();
        if (acc != null && acc.getEmail() != null) {
            StoreStatusChangedData mailData = StoreStatusChangedData.builder()
                    .email(acc.getEmail())
                    .ownerName(acc.getName())
                    .storeName(store.getStoreName())
                    .newStatus(StoreStatus.ACTIVE.name())
                    .reason("Cửa hàng đã thanh toán nợ và đáp ứng điều kiện an toàn. Hệ thống đã tự động mở khóa.")
                    .siteUrl(siteUrl)
                    .build();
            try {
                emailService.sendEmail(EmailTemplateType.STORE_STATUS_UPDATED, mailData);
            } catch (MessagingException e) {
                log.error("[UNLOCK][EMAIL-FAIL] storeId={}", storeId, e);
            }
        }

        log.warn("[UNLOCK-SUCCESS] storeId={} debt={} deposit={} limit={}",
                storeId, debt, deposit, adjustedLimit);

        return true;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
