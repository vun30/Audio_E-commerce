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

    // 1 legalPoint = +100,000 tín dụng
    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");

    // Ngưỡng block = 100%
    private static final BigDecimal BLOCK_RATIO = BigDecimal.ONE;

    @Value("${app.site-url}")
    private String siteUrl;

    @Transactional
    public int scanAndBlockStoresByDebt() {

        List<Store> stores = storeRepository.findAllActiveWithWallet(StoreStatus.ACTIVE);

        int blockedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Store store : stores) {

            StoreWallet wallet = store.getWallet();
            if (wallet == null) continue;

            BigDecimal debt = nz(wallet.getDebtBalance());
            if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal deposit = nz(wallet.getDepositBalance());
            BigDecimal legalPoint = nz(store.getLegalPoint());

            BigDecimal creditBonus = legalPoint.multiply(LEGAL_BONUS_UNIT);
            BigDecimal adjustedLimit = deposit.add(creditBonus); // hạn mức = cọc + tín dụng

            boolean shouldBlock;
            BigDecimal ratio = BigDecimal.ZERO;

            if (adjustedLimit.compareTo(BigDecimal.ZERO) <= 0) {
                shouldBlock = true;
                ratio = BigDecimal.ONE; // coi như >=100%
            } else {
                ratio = debt.divide(adjustedLimit, 4, RoundingMode.HALF_UP);
                shouldBlock = ratio.compareTo(BLOCK_RATIO) >= 0; // >= 100%
            }

            if (!shouldBlock) continue;

            // Nếu store đã bị khóa vì nợ trước đó thì bỏ qua (tránh gửi mail spam)
            if (store.getStatus() == StoreStatus.SUSPENDED_DEBT) continue;

            // ====== 1) BLOCK STORE ======
            store.setStatus(StoreStatus.SUSPENDED_DEBT);
            store.setLastRiskWarningAt(now);

            // ====== 2) UPDATE PRODUCTS ======
            int movedActive = productRepository.bulkUpdateStatusByStoreAndStatus(
                    store.getStoreId(),
                    ProductStatus.ACTIVE,
                    ProductStatus.SUSPENDED_DEBT
            );

            int movedUnlisted = productRepository.bulkUpdateStatusByStoreAndStatus(
                    store.getStoreId(),
                    ProductStatus.UNLISTED,
                    ProductStatus.UNLISTED_BEFORE_SUSPENDED_DEBT
            );

            // ====== 3) SEND EMAIL TO STORE OWNER ======
            Account acc = store.getAccount(); // store có account @OneToOne
            if (acc != null && acc.getEmail() != null && !acc.getEmail().isBlank()) {
                String reason = buildDebtReason(debt, deposit, legalPoint, creditBonus, adjustedLimit, ratio);

                StoreStatusChangedData mailData = StoreStatusChangedData.builder()
                        .email(acc.getEmail())
                        .ownerName(acc.getName() != null ? acc.getName() : store.getStoreName())
                        .storeName(store.getStoreName())
                        .newStatus(StoreStatus.SUSPENDED_DEBT.name())
                        .reason(reason)
                        .siteUrl(siteUrl)
                        .build();

                try {
                    // @Async trong EmailService => không chặn luồng xử lý
                    emailService.sendEmail(EmailTemplateType.STORE_STATUS_UPDATED, mailData);
                } catch (MessagingException e) {
                    log.error("[DEBT-BLOCK][EMAIL-FAIL] storeId={} email={} err={}",
                            store.getStoreId(), acc.getEmail(), e.getMessage(), e);
                }
            }

            log.warn("[DEBT-BLOCK] storeId={} debt={} deposit={} legalPoint={} limit={} ratio={} movedActive={} movedUnlisted={}",
                    store.getStoreId(), debt, deposit, legalPoint, adjustedLimit, ratio, movedActive, movedUnlisted);

            blockedCount++;
        }

        return blockedCount;
    }

    private String buildDebtReason(BigDecimal debt,
                                   BigDecimal deposit,
                                   BigDecimal legalPoint,
                                   BigDecimal creditBonus,
                                   BigDecimal limit,
                                   BigDecimal ratio) {

        // Bạn có thể format tiền VNĐ ở FE; ở đây mình ghi rõ số để audit.
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
