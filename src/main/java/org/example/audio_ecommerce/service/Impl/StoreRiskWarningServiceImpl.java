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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRiskWarningServiceImpl implements StoreRiskWarningService {

    private static final BigDecimal R20 = new BigDecimal("0.20");
    private static final BigDecimal R50 = new BigDecimal("0.50");
    private static final BigDecimal R80 = new BigDecimal("0.80");

    // ✅ 1 legalPoint = +100,000 nới ngưỡng nợ
    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");

    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public int runEarlyWarningScan() {

        List<Store> stores = storeRepository.findStoresWithWalletByStatuses(
                List.of(StoreStatus.ACTIVE, StoreStatus.PAUSED, StoreStatus.SUSPENDED_DEBT)
        );
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Store s : stores) {
            StoreWallet w = s.getWallet();
            if (w == null) continue;

            BigDecimal debt = nz(w.getDebtBalance());
            BigDecimal deposit = nz(w.getDepositBalance());

            BigDecimal legalPoint = nz(s.getLegalPoint());
            BigDecimal bonus = legalPoint.multiply(LEGAL_BONUS_UNIT);
            BigDecimal adjustedDeposit = deposit.add(bonus);

            StoreRiskWarningLevel newLevel = calcLevel(debt, adjustedDeposit);

            StoreRiskWarningLevel oldLevel =
                    s.getRiskWarningLevel() == null ? StoreRiskWarningLevel.NONE : s.getRiskWarningLevel();

            if (newLevel != oldLevel) {
                s.setRiskWarningLevel(newLevel);
                s.setLastRiskWarningAt(now);

                log.warn("[RISK] storeId={} level={} debt={} deposit={} legalPoint={} adjustedDeposit={}",
                        s.getStoreId(), newLevel, debt, deposit, legalPoint, adjustedDeposit);

                updated++;
            }
        }
        return updated;
    }

    // ✅ calc theo debt / adjustedDeposit
    private StoreRiskWarningLevel calcLevel(BigDecimal debt, BigDecimal adjustedDeposit) {
        debt = nz(debt);
        adjustedDeposit = nz(adjustedDeposit);

        if (debt.compareTo(BigDecimal.ZERO) <= 0) return StoreRiskWarningLevel.NONE;

        // không có cọc + không có bonus mà có nợ => nguy hiểm
        if (adjustedDeposit.compareTo(BigDecimal.ZERO) <= 0) return StoreRiskWarningLevel.BLOCK_100;

        BigDecimal ratio = debt.divide(adjustedDeposit, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(R80) >= 0) return StoreRiskWarningLevel.DANGER_80;
        if (ratio.compareTo(R50) >= 0) return StoreRiskWarningLevel.WARNING_50;
        if (ratio.compareTo(R20) >= 0) return StoreRiskWarningLevel.DEBT_NOTICE_20; // NOTICE map chung
        return StoreRiskWarningLevel.NONE;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
