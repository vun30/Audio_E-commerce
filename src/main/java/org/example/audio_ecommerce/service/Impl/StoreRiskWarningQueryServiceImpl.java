package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.Enum.StoreRiskWarningLevel;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StoreRiskWarningQueryServiceImpl implements StoreRiskWarningQueryService {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private static final BigDecimal WARNING_RATIO  = new BigDecimal("0.80");
    private static final BigDecimal CRITICAL_RATIO = new BigDecimal("0.90");

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreRiskWarningResponse getMyRiskWarning() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        BigDecimal legalPoint = nz(store.getLegalPoint());
        BigDecimal creditLimit = legalPoint.multiply(ONE_MILLION);

        BigDecimal debt = nz(wallet.getDebtBalance());
        BigDecimal deposit = nz(wallet.getDepositBalance());

        BigDecimal effectiveDebt = debt.subtract(deposit);
        if (effectiveDebt.compareTo(BigDecimal.ZERO) < 0) effectiveDebt = BigDecimal.ZERO;

        BigDecimal warningLine = creditLimit.multiply(WARNING_RATIO);
        BigDecimal criticalLine = creditLimit.multiply(CRITICAL_RATIO);

        StoreRiskWarningLevel level = StoreRiskWarningLevel.NONE;
        if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
            if (effectiveDebt.compareTo(criticalLine) >= 0) level = StoreRiskWarningLevel.CRITICAL_90;
            else if (effectiveDebt.compareTo(warningLine) >= 0) level = StoreRiskWarningLevel.WARNING_80;
        }

        return StoreRiskWarningResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .storeStatus(store.getStatus())
                .warningLevel(level)
                .legalPoint(legalPoint)
                .creditLimit(creditLimit)
                .debtBalance(debt)
                .depositBalance(deposit)
                .effectiveDebt(effectiveDebt)
                .warningLine(warningLine)
                .criticalLine(criticalLine)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
