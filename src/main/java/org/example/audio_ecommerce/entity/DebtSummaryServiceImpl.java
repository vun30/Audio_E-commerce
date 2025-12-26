package org.example.audio_ecommerce.entity;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.DebtAmountBreakdownDto;
import org.example.audio_ecommerce.dto.response.GhnFlatDebtSummaryResponse;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.DebtSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DebtSummaryServiceImpl implements DebtSummaryService {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;

    @Override
    @Transactional(readOnly = true)
    public GhnFlatDebtSummaryResponse calcGhnFlatDebtSummary(UUID storeId) {

        // ✅ Load list (không dùng SUM query)
        List<StoreOrder> orders = (storeId == null)
                ? storeOrderRepository.findAll()
                : storeOrderRepository.findAllByStore_StoreId(storeId);

        List<ReturnShippingFee> returnFees = (storeId == null)
                ? returnShippingFeeRepository.findAll()
                : returnShippingFeeRepository.findAllByStoreId(storeId);

        BigDecimal customerPaidTotal = BigDecimal.ZERO;

        BigDecimal storeOrderDebtTotal = BigDecimal.ZERO;
        BigDecimal storeOrderDebtPaid = BigDecimal.ZERO;
        BigDecimal storeOrderDebtOutstanding = BigDecimal.ZERO;

        BigDecimal returnDebtTotal = BigDecimal.ZERO;
        BigDecimal returnDebtPaid = BigDecimal.ZERO;
        BigDecimal returnDebtOutstanding = BigDecimal.ZERO;

        // =========================
        // 1) ORDERS
        // =========================
        for (StoreOrder o : orders) {

            // rule: chỉ tính đơn có ngày deli
            if (o.getDeliveredAt() == null) continue;

            BigDecimal shipReal = nz(o.getShippingFeeReal());
            if (shipReal.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal shipEstimated = nz(o.getShippingFee());

            boolean returnApplied = Boolean.TRUE.equals(o.getReturnChargeApplied());
            boolean paidByShop = Boolean.TRUE.equals(o.getPaidByShop());

            // (A) Tổng cus đã trả = ship dự kiến
            customerPaidTotal = customerPaidTotal.add(shipEstimated);

            // (B) shop nợ flat
            BigDecimal shopDebt = returnApplied
                    ? shipReal.multiply(new BigDecimal("1.5"))
                    : shipReal.subtract(shipEstimated).max(BigDecimal.ZERO);

            shopDebt = shopDebt.setScale(2, RoundingMode.HALF_UP);

            storeOrderDebtTotal = storeOrderDebtTotal.add(shopDebt);

            if (paidByShop) {
                storeOrderDebtPaid = storeOrderDebtPaid.add(shopDebt);
            } else {
                storeOrderDebtOutstanding = storeOrderDebtOutstanding.add(shopDebt);
            }
        }

        // =========================
        // 2) RETURN SHIPPING FEES
        // =========================
        for (ReturnShippingFee f : returnFees) {

            // chỉ phí shop chịu
            if (f.getPayer() != null && !"SHOP".equalsIgnoreCase(f.getPayer())) continue;

            BigDecimal amt = nz(f.getChargedToShop());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                amt = nz(f.getShippingFee());
            }
            if (amt.compareTo(BigDecimal.ZERO) <= 0) continue;

            boolean paidByShop = Boolean.TRUE.equals(f.getPaidByShop());

            returnDebtTotal = returnDebtTotal.add(amt);

            if (paidByShop) {
                returnDebtPaid = returnDebtPaid.add(amt);
            } else {
                returnDebtOutstanding = returnDebtOutstanding.add(amt);
            }
        }

        // =========================
        // 3) FLAT NỢ GHN (đúng công thức bạn yêu cầu)
        // =========================
        BigDecimal flatDebtToGHN =
                customerPaidTotal
                        .add(storeOrderDebtTotal)
                        .add(returnDebtTotal);

        String scope = (storeId == null) ? "ALL_SYSTEM" : "STORE_ONLY";

        return GhnFlatDebtSummaryResponse.builder()
                .scope(scope)
                .storeId(storeId)
                .flatDebtToGHN(flatDebtToGHN)
                .customerPaidTotal(customerPaidTotal)
                .storeOrderDebtToFlat(DebtAmountBreakdownDto.builder()
                        .total(storeOrderDebtTotal)
                        .paid(storeOrderDebtPaid)
                        .outstanding(storeOrderDebtOutstanding)
                        .build())
                .returnFeeDebtToFlat(DebtAmountBreakdownDto.builder()
                        .total(returnDebtTotal)
                        .paid(returnDebtPaid)
                        .outstanding(returnDebtOutstanding)
                        .build())
                .formula("flatDebtToGHN = customerPaidTotal + storeOrderDebtToFlat.total + returnFeeDebtToFlat.total")
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
