package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.Enum.StoreRiskWarningLevel;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreRiskWarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRiskWarningServiceImpl implements StoreRiskWarningService {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    // ngưỡng cảnh báo (bạn có thể chuyển sang config)
    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.80");
    private static final BigDecimal CRITICAL_RATIO = new BigDecimal("0.90");

    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public int runEarlyWarningScan() {
        List<Store> stores = storeRepository.findAllActiveWithWallet(StoreStatus.ACTIVE);

        int warnedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Store s : stores) {
            StoreWallet w = s.getWallet();
            if (w == null) continue;

            BigDecimal legalPoint = nz(s.getLegalPoint());
            BigDecimal creditLimit = legalPoint.multiply(ONE_MILLION);

            if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
                // legalPoint=0 => limit=0, tuỳ bạn: bỏ qua hoặc cảnh báo luôn
                continue;
            }

            BigDecimal debt = nz(w.getDebtBalance());
            BigDecimal deposit = nz(w.getDepositBalance());

            // effectiveDebt = max(0, debt - deposit)
            BigDecimal effectiveDebt = debt.subtract(deposit);
            if (effectiveDebt.compareTo(BigDecimal.ZERO) < 0) effectiveDebt = BigDecimal.ZERO;

            BigDecimal warningLine = creditLimit.multiply(WARNING_RATIO);
            BigDecimal criticalLine = creditLimit.multiply(CRITICAL_RATIO);

            StoreRiskWarningLevel level = StoreRiskWarningLevel.NONE;

            if (effectiveDebt.compareTo(criticalLine) >= 0) {
                level = StoreRiskWarningLevel.CRITICAL_90;
            } else if (effectiveDebt.compareTo(warningLine) >= 0) {
                level = StoreRiskWarningLevel.WARNING_80;
            }

            // Nếu muốn chống spam: chỉ update khi level thay đổi hoặc sau X giờ
            StoreRiskWarningLevel old = s.getRiskWarningLevel() == null ? StoreRiskWarningLevel.NONE : s.getRiskWarningLevel();

            if (level != old) {
                s.setRiskWarningLevel(level);
                s.setLastRiskWarningAt(now);

                // TODO: gọi NotificationService (email/in-app) nếu bạn có
                log.warn("[RISK-WARN] storeId={} level={} effectiveDebt={} limit={} (debt={}, deposit={})",
                        s.getStoreId(), level, effectiveDebt, creditLimit, debt, deposit);

                warnedCount++;
            }
        }

        return warnedCount;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
