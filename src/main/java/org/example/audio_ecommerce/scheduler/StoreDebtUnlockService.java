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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreDebtUnlockService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final TaskScheduler taskScheduler;

    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");
    private static final BigDecimal SAFE_DEPOSIT_RATIO = new BigDecimal("0.10"); // 10%

    @Value("${app.site-url:}")
    private String siteUrl;

    /**
     * Được gọi từ API thanh toán nợ
     * → delay 3 phút rồi mới check unlock
     */
    public void scheduleUnlockCheck(UUID storeId) {
        taskScheduler.schedule(
                () -> tryUnlockStore(storeId),
                Instant.now().plusSeconds(180) // ⏱️ delay 3 phút
        );
    }

    @Transactional
    public void tryUnlockStore(UUID storeId) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return;

/**
 * ❌ BỎ QUA STORE BỊ SUSPEND VĨNH VIỄN / ADMIN
 */
        if (store.getStatus() == StoreStatus.SUSPENDED
                || store.getStatus() == StoreStatus.ABANDONED
                || store.getStatus() == StoreStatus.REJECTED) {

            log.info("[UNLOCK-SKIP][PERMANENT] storeId={} status={}",
                    storeId, store.getStatus());
            return;
        }

/**
 * ✅ CHỈ MỞ KHÓA NẾU BỊ KHÓA DO NỢ
 */
        if (store.getStatus() != StoreStatus.SUSPENDED_DEBT) return;

        StoreWallet wallet = store.getWallet();
        if (wallet == null) return;

        BigDecimal debt = nz(wallet.getDebtBalance());
        BigDecimal deposit = nz(wallet.getDepositBalance());
        BigDecimal legalPoint = nz(store.getLegalPoint());

        BigDecimal adjustedLimit =
                deposit.add(legalPoint.multiply(LEGAL_BONUS_UNIT));

// ===== ĐIỀU KIỆN MỞ KHÓA =====
        boolean passDebtLimit = debt.compareTo(adjustedLimit) < 0;
        boolean passDepositSafe =
                deposit.compareTo(debt.multiply(SAFE_DEPOSIT_RATIO)) >= 0;

        if (!passDebtLimit || !passDepositSafe) {
            log.info("[UNLOCK-SKIP] storeId={} debt={} deposit={} limit={}",
                    storeId, debt, deposit, adjustedLimit);
            return;
        }

        // ===== MỞ KHÓA STORE =====
        store.setStatus(StoreStatus.ACTIVE);
        store.setLastRiskWarningAt(LocalDateTime.now());

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

        // ===== GỬI MAIL MỞ KHÓA =====
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

        log.warn("[UNLOCK-SUCCESS] storeId={} debt={} deposit={}",
                storeId, debt, deposit);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
