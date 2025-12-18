package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreDebtReportResponse;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreReportServiceImpl implements StoreReportService {

    private final StoreRepository storeRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreDebtReportResponse getDebtReport(UUID storeId, LocalDateTime from, LocalDateTime to) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store: " + storeId));

        // 1) Load orders của store (ưu tiên filter paidByShop=false nếu bạn dùng flag này ở StoreOrder)
        // Bạn đổi lại method repo cho đúng DB bạn đang có.
        List<StoreOrder> orders = findOrders(storeId, from, to);

        // 2) Load return fees (payer=SHOP & paidByShop=false)
        List<ReturnShippingFee> returnFees = findReturnFees(storeId, from, to);

        // 3) Map orders -> report items + tính tổng
        BigDecimal debtFromOrders = BigDecimal.ZERO;
        List<StoreDebtReportResponse.OrderDebtItem> orderItems = new ArrayList<>();

        for (StoreOrder o : orders) {
            BigDecimal shippingReal = nz(o.getShippingFeeReal());
            BigDecimal shippingEst  = nz(o.getShippingFee());

            boolean delivered = o.getDeliveredAt() != null;
            boolean returnApplied = Boolean.TRUE.equals(o.getReturnChargeApplied());

            BigDecimal baseDebt;
            BigDecimal extraDiff = BigDecimal.ZERO;
            BigDecimal returnDebt = BigDecimal.ZERO;

            String ruleApplied;

            if (returnApplied) {
                // RETURNED / không nhận: debt = real + 50%*real, KHÔNG tính chênh lệch
                baseDebt = shippingReal;
                returnDebt = percent(shippingReal, nz(o.getReturnShippingChargeRate())); // default 50
                extraDiff = BigDecimal.ZERO;
                ruleApplied = "RETURNED(returnChargeApplied=true): debt = real + 50%*real (NO extra-diff)";
            } else if (delivered) {
                // DELIVERED: debt = max(0, real - est)
                BigDecimal diff = shippingReal.subtract(shippingEst);
                if (diff.compareTo(BigDecimal.ZERO) < 0) diff = BigDecimal.ZERO;
                baseDebt = diff;
                extraDiff = diff;
                ruleApplied = "DELIVERED: debt = max(0, real - estimated)";
            } else {
                // Chưa delivered, chưa return: theo logic nợ ship thực tế (tuỳ bạn)
                baseDebt = shippingReal;
                extraDiff = BigDecimal.ZERO;
                ruleApplied = "IN_TRANSIT: debt = real (not delivered yet)";
            }

            BigDecimal totalDebtOrder = baseDebt.add(returnDebt);

            // gợi ý: ghi snapshot returnShippingCharge để FE hiển thị
            BigDecimal returnShippingCharge = returnApplied ? returnDebt : BigDecimal.ZERO;

            StoreDebtReportResponse.OrderDebtItem item =
                    StoreDebtReportResponse.OrderDebtItem.builder()
                            .storeOrderId(o.getId())
                            .orderCode(o.getOrderCode())
                            .status(String.valueOf(o.getStatus()))
                            .createdAt(toOffset(o.getCreatedAt()))
                            .deliveredAt(toOffset(o.getDeliveredAt()))
                            .paidByShop(Boolean.TRUE.equals(o.getPaidByShop()))

                            .shipping(StoreDebtReportResponse.Shipping.builder()
                                    .shippingFeeEstimated(shippingEst)
                                    .shippingFeeReal(shippingReal)

                                    .returnChargeApplied(returnApplied)
                                    .returnChargeRate(nz(o.getReturnShippingChargeRate()))
                                    .returnShippingCharge(returnShippingCharge)

                                    .shippingExtraForStore(extraDiff)
                                    .ruleApplied(ruleApplied)
                                    .build())

                            .debtDetail(StoreDebtReportResponse.DebtDetail.builder()
                                    .baseDebt(baseDebt)
                                    .returnDebt(returnApplied ? returnDebt : null)
                                    .totalDebtOrder(totalDebtOrder)
                                    .build())
                            .build();

            // Chỉ cộng nợ các order chưa paidByShop (đúng yêu cầu nợ outstanding)
            if (!Boolean.TRUE.equals(o.getPaidByShop())) {
                debtFromOrders = debtFromOrders.add(totalDebtOrder);
            }

            orderItems.add(item);
        }

        // 4) Map return fees
        BigDecimal debtFromReturnFees = BigDecimal.ZERO;
        List<StoreDebtReportResponse.ReturnFeeItem> feeItems = new ArrayList<>();

        for (ReturnShippingFee f : returnFees) {
            BigDecimal charged = nz(f.getChargedToShop());
            BigDecimal fee = nz(f.getShippingFee());

            BigDecimal amount = charged.compareTo(BigDecimal.ZERO) > 0 ? charged : fee;
            debtFromReturnFees = debtFromReturnFees.add(amount);

            feeItems.add(StoreDebtReportResponse.ReturnFeeItem.builder()
                    .id(f.getId())
                    .returnRequestId(f.getReturnRequestId())
                    .payer(f.getPayer())
                    .paidByShop(Boolean.TRUE.equals(f.getPaidByShop()))
                    .ghnOrderCode(f.getGhnOrderCode())
                    .shippingFee(fee)
                    .chargedToShop(charged)
                    .ruleApplied("ReturnShippingFee: payer=SHOP & paidByShop=false => debt += chargedToShop (or shippingFee)")
                    .build());
        }

        BigDecimal totalDebt = debtFromOrders.add(debtFromReturnFees);

        return StoreDebtReportResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .currency("VND")
                .evaluatedAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))

                .summary(StoreDebtReportResponse.Summary.builder()
                        .totalDebt(totalDebt)
                        .debtFromOrders(debtFromOrders)
                        .debtFromReturnFees(debtFromReturnFees)
                        .debtOrdersCount((int) orderItems.stream()
                                .filter(x -> Boolean.FALSE.equals(x.getPaidByShop()))
                                .count())
                        .returnFeesCount(feeItems.size())
                        .build())
                .orders(orderItems)
                .returnShippingFees(feeItems)
                .build();
    }

    // ===== Helpers =====

    private List<StoreOrder> findOrders(UUID storeId, LocalDateTime from, LocalDateTime to) {
        // Bạn thay bằng repo query thật:
        // - ưu tiên lọc paidByShop=false
        // - filter thời gian theo createdAt (nếu cần)
        if (from == null && to == null) {
            return storeOrderRepository.findByStore_StoreIdAndPaidByShopFalse(storeId);
        }
        return storeOrderRepository.findByStore_StoreIdAndPaidByShopFalseAndCreatedAtBetween(
                storeId,
                from != null ? from : LocalDateTime.of(1970,1,1,0,0),
                to != null ? to : LocalDateTime.of(2999,12,31,23,59)
        );
    }

    private List<ReturnShippingFee> findReturnFees(UUID storeId, LocalDateTime from, LocalDateTime to) {
        // payer="SHOP" & paidByShop=false
        if (from == null && to == null) {
            return returnShippingFeeRepository.findByStoreIdAndPayerAndPaidByShopFalse(storeId, "SHOP");
        }
        return returnShippingFeeRepository.findByStoreIdAndPayerAndPaidByShopFalseAndCreatedAtBetween(
                storeId,
                "SHOP",
                from != null ? from : LocalDateTime.of(1970,1,1,0,0),
                to != null ? to : LocalDateTime.of(2999,12,31,23,59)
        );
    }

    private BigDecimal percent(BigDecimal base, BigDecimal ratePercent) {
        if (base == null) return BigDecimal.ZERO;
        BigDecimal r = nz(ratePercent);
        if (r.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return base.multiply(r).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private OffsetDateTime toOffset(LocalDateTime t) {
        if (t == null) return null;
        return t.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
    }
}
