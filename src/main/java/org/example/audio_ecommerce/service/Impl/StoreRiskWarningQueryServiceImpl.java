package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.Enum.StoreRiskWarningLevel;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreRiskWarningQueryServiceImpl implements StoreRiskWarningQueryService {

    private static final BigDecimal R20 = new BigDecimal("0.20");
    private static final BigDecimal R50 = new BigDecimal("0.50");
    private static final BigDecimal R80 = new BigDecimal("0.80");

    // ✅ 1 legalPoint = +100,000 nới ngưỡng nợ
    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreRiskWarningResponse getRiskWarningByStoreId(UUID storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store"));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Store chưa có ví"));

        BigDecimal debt = nz(wallet.getDebtBalance());
        BigDecimal deposit = nz(wallet.getDepositBalance());

        // ✅ legalPoint dùng để nới hạn mức nợ
        BigDecimal legalPoint = nz(store.getLegalPoint());
        BigDecimal bonus = legalPoint.multiply(LEGAL_BONUS_UNIT);

        // ✅ Ngưỡng so sánh thực tế = deposit + bonus
        BigDecimal adjustedDeposit = deposit.add(bonus);

        // effectiveDebt = debt (theo logic mới)
        BigDecimal effectiveDebt = debt;

        StoreRiskWarningLevel level = calcLevel(debt, adjustedDeposit);

        return StoreRiskWarningResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .storeStatus(store.getStatus())

                .warningLevel(level)

                // ✅ giữ DTO cũ nhưng map đúng nghĩa
                .legalPoint(legalPoint)            // điểm legal thật
                .creditLimit(adjustedDeposit)      // "hạn mức" mới = deposit + bonus

                .debtBalance(debt)
                .depositBalance(deposit)
                .effectiveDebt(effectiveDebt)

                // ✅ ngưỡng theo adjustedDeposit
                .warningLine(adjustedDeposit.multiply(R20))   // 20%
                .criticalLine(adjustedDeposit.multiply(R80))  // 80%

                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    // ✅ calc theo debt / adjustedDeposit
    private StoreRiskWarningLevel calcLevel(BigDecimal debt, BigDecimal adjustedDeposit) {
        debt = nz(debt);
        adjustedDeposit = nz(adjustedDeposit);

        if (debt.compareTo(BigDecimal.ZERO) <= 0) return StoreRiskWarningLevel.NONE;

        // không có cọc + không có bonus mà có nợ => nguy hiểm
        if (adjustedDeposit.compareTo(BigDecimal.ZERO) <= 0) return StoreRiskWarningLevel.CRITICAL_90;

        BigDecimal ratio = debt.divide(adjustedDeposit, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(R80) >= 0) return StoreRiskWarningLevel.CRITICAL_90;
        if (ratio.compareTo(R50) >= 0) return StoreRiskWarningLevel.WARNING_80;
        if (ratio.compareTo(R20) >= 0) return StoreRiskWarningLevel.WARNING_80; // NOTICE map chung
        return StoreRiskWarningLevel.NONE;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
