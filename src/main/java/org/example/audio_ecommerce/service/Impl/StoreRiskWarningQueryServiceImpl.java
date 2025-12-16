package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.Enum.StoreRiskWarningLevel;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
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

    private static final BigDecimal R20  = new BigDecimal("0.20");
    private static final BigDecimal R50  = new BigDecimal("0.50");
    private static final BigDecimal R80  = new BigDecimal("0.80");
    private static final BigDecimal R90  = new BigDecimal("0.90");
    private static final BigDecimal R100 = new BigDecimal("1.00");

    // ✅ 1 legalPoint = +100,000 nới hạn mức nợ
    private static final BigDecimal LEGAL_BONUS_UNIT = new BigDecimal("100000");

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;

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

        // ✅ Hạn mức thực tế = deposit + bonus
        BigDecimal creditLimit = deposit.add(bonus);

        // ✅ effectiveDebt hiện tại = debt (sau này có thể đổi mà FE không cần sửa)
        BigDecimal effectiveDebt = debt;

        StoreRiskWarningLevel level = calcLevel(effectiveDebt, creditLimit);

        // ==========================================================
        // ✅ NEW: Tổng nợ có thể thanh toán ngay (NOW)
        // = unpaid final orders + unpaid return fees (shop chịu)
        // ==========================================================
        BigDecimal payableOrderDebt = storeOrderRepository
                .findUnpaidFinalOrdersOfStore(storeId)
                .stream()
                .map(o -> nz(o.getTotalDebtOrder()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payableReturnFeeDebt = returnShippingFeeRepository
                .findUnpaidShopReturnFees(storeId)
                .stream()
                .map(f -> nz(f.getChargedToShop()).compareTo(BigDecimal.ZERO) > 0
                        ? nz(f.getChargedToShop())
                        : nz(f.getShippingFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payableNowDebt = payableOrderDebt.add(payableReturnFeeDebt);

        return StoreRiskWarningResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .storeStatus(store.getStatus())

                .warningLevel(level)

                // ✅ giữ DTO cũ nhưng map đúng nghĩa
                .legalPoint(legalPoint)
                .creditLimit(creditLimit)

                .debtBalance(debt)
                .depositBalance(deposit)
                .effectiveDebt(effectiveDebt)

                // ✅ NEW FIELD
                .payableNowDebt(payableNowDebt)

                // ✅ ngưỡng theo creditLimit
                .warningLine(creditLimit.multiply(R20))   // 20%
                .criticalLine(creditLimit.multiply(R80))  // 80%

                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private StoreRiskWarningLevel calcLevel(BigDecimal effectiveDebt, BigDecimal creditLimit) {
        effectiveDebt = nz(effectiveDebt);
        creditLimit = nz(creditLimit);

        if (effectiveDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return StoreRiskWarningLevel.NONE;
        }

        if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return StoreRiskWarningLevel.BLOCK_100;
        }

        BigDecimal ratio = effectiveDebt.divide(creditLimit, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(R100) >= 0) return StoreRiskWarningLevel.BLOCK_100;
        if (ratio.compareTo(R90)  >= 0) return StoreRiskWarningLevel.CRITICAL_90;
        if (ratio.compareTo(R80)  >= 0) return StoreRiskWarningLevel.DANGER_80;
        if (ratio.compareTo(R50)  >= 0) return StoreRiskWarningLevel.WARNING_50;
        if (ratio.compareTo(R20)  >= 0) return StoreRiskWarningLevel.DEBT_NOTICE_20;

        return StoreRiskWarningLevel.NONE;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}