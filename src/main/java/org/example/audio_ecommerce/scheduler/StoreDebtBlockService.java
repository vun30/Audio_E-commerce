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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreDebtBlockService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");
    private static final BigDecimal BLOCK_RATIO = BigDecimal.ONE; // 100%

    @Value("${app.site-url:}")
    private String siteUrl;

    @Scheduled(cron = "*/30 * * * * *") // mỗi 30 giây
//    @Transactional
    public void scanAndBlockStoresByDebt() {

        LocalDateTime now = LocalDateTime.now();
        log.info("[DEBT-BLOCK] tick at {}", now);

        List<Store> stores = storeRepository.findStoresWithWalletByStatuses(
                List.of(StoreStatus.ACTIVE, StoreStatus.PAUSED)
        );

        if (stores == null || stores.isEmpty()) {
            log.info("[DEBT-BLOCK] no ACTIVE/PAUSED store to scan");
            return;
        }

        int blockedCount = 0;

        for (Store store : stores) {
            try {
                StoreWallet wallet = store.getWallet();
                if (wallet == null) {
                    log.warn("[DEBT-BLOCK] skip storeId={} reason=wallet_null", store.getStoreId());
                    continue;
                }

                BigDecimal debt = nz(wallet.getDebtBalance());
                if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal deposit = nz(wallet.getDepositBalance());
                BigDecimal legalPoint = nz(store.getLegalPoint());

                BigDecimal creditBonus = legalPoint.multiply(LEGAL_BONUS_UNIT);
                BigDecimal creditLimit = deposit.add(creditBonus);

                BigDecimal ratio;
                boolean shouldBlock;

                if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
                    shouldBlock = true;
                    ratio = BigDecimal.ONE;
                } else {
                    ratio = debt.divide(creditLimit, 4, RoundingMode.HALF_UP);
                    shouldBlock = ratio.compareTo(BLOCK_RATIO) >= 0;
                }

                if (!shouldBlock) continue;

                // idempotent
                if (store.getStatus() == StoreStatus.SUSPENDED_DEBT) continue;

                // lưu oldStatus cho log
                StoreStatus oldStatus = store.getStatus();

                // ====== 1) LOCK SHOP ======
                store.setStatus(StoreStatus.SUSPENDED_DEBT);
                store.setLastRiskWarningAt(now);
                storeRepository.save(store);

                // ====== 2) UPDATE PRODUCTS (FAIL KHÔNG CHẶN EMAIL) ======
                int movedActive = 0;
                int movedUnlisted = 0;
                try {
                    movedActive = productRepository.bulkUpdateStatusByStoreAndStatus(
                            store.getStoreId(),
                            ProductStatus.ACTIVE,
                            ProductStatus.SUSPENDED_DEBT
                    );

                    movedUnlisted = productRepository.bulkUpdateStatusByStoreAndStatus(
                            store.getStoreId(),
                            ProductStatus.UNLISTED,
                            ProductStatus.UNLISTED_BEFORE_SUSPENDED_DEBT
                    );
                } catch (Exception e) {
                    // ✅ vẫn tiếp tục gửi mail dù update product lỗi
                    log.error("[DEBT-BLOCK][PRODUCT-UPDATE-FAIL] storeId={} err={}",
                            store.getStoreId(), e.getMessage(), e);
                }

                // ====== 3) SEND EMAIL (LUÔN CHẠY) ======
                Account acc = store.getAccount();
                if (acc != null && acc.getEmail() != null && !acc.getEmail().isBlank()) {
                    String reason = buildDebtReason(debt, deposit, legalPoint, creditBonus, creditLimit, ratio);

                    StoreStatusChangedData mailData = StoreStatusChangedData.builder()
                            .email(acc.getEmail())
                            .ownerName(acc.getName() != null ? acc.getName() : store.getStoreName())
                            .storeName(store.getStoreName())
                            .newStatus(StoreStatus.SUSPENDED_DEBT.name())
                            .reason(reason)
                            .siteUrl(siteUrl)
                            .build();

                    try {
                        emailService.sendEmail(EmailTemplateType.STORE_STATUS_UPDATED, mailData);
                        log.info("[DEBT-BLOCK][EMAIL-SENT] storeId={} email={}", store.getStoreId(), acc.getEmail());
                    } catch (MessagingException e) {
                        log.error("[DEBT-BLOCK][EMAIL-FAIL] storeId={} email={} err={}",
                                store.getStoreId(), acc.getEmail(), e.getMessage(), e);
                    }
                } else {
                    log.warn("[DEBT-BLOCK] storeId={} no email to notify", store.getStoreId());
                }

                log.warn("[DEBT-BLOCK][BLOCKED] storeId={} oldStatus={} newStatus={} debt={} deposit={} legalPoint={} limit={} ratio={} movedActive={} movedUnlisted={}",
                        store.getStoreId(), oldStatus, store.getStatus(), debt, deposit, legalPoint, creditLimit, ratio, movedActive, movedUnlisted);

                blockedCount++;

            } catch (Exception ex) {
                log.error("[DEBT-BLOCK][ERROR] storeId={} err={}", store.getStoreId(), ex.getMessage(), ex);
            }
        }

        if (blockedCount > 0) {
            log.warn("[DEBT-BLOCK] DONE blockedCount={}", blockedCount);
        }
    }

    private String buildDebtReason(BigDecimal debt,
                                   BigDecimal deposit,
                                   BigDecimal legalPoint,
                                   BigDecimal creditBonus,
                                   BigDecimal limit,
                                   BigDecimal ratio) {

        return "Cửa hàng bị tạm khóa do vượt ngưỡng nợ cho phép (100%). "
                + "Nợ hiện tại: " + debt + ". "
                + "Tiền cọc: " + deposit + ". "
                + "Điểm uy tín (legalPoint): " + legalPoint + " => tín dụng thêm: " + creditBonus + ". "
                + "Hạn mức cho phép (cọc + tín dụng): " + limit + ". "
                + "Tỷ lệ nợ/hạn mức: " + ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%.";
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
