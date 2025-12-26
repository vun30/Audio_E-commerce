package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreOrderDebtItemResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StoreDebtQueryService {

    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;

    // bạn chỉnh end status ở đây
    private static final EnumSet<OrderStatus> END_STATUSES = EnumSet.of(
            OrderStatus.DELIVERY_SUCCESS,
            OrderStatus.RETURNED,
            OrderStatus.RETURNING
    );

    @Transactional(readOnly = true)
    public List<StoreOrderDebtItemResponse> getUnpaidEndDebtOrders(UUID storeId) {

        // 1) Nợ từ StoreOrder (end statuses, unpaid)
        List<StoreOrderDebtItemResponse> orderDebts =
                storeOrderRepository.findUnpaidDebtEndOrdersByStore(
                        storeId,
                        List.copyOf(END_STATUSES)
                ).stream().map(this::toDebtItem).toList();

        // 2) Nợ từ ReturnShippingFee (payer=SHOP, unpaid)
        List<StoreOrderDebtItemResponse> returnDebts =
                returnShippingFeeRepository.findUnpaidShopReturnFeesByStoreId(storeId)
                        .stream()
                        .map(this::toReturnDebtItem)
                        .toList();

        // 3) Gộp lại 1 list
        return Stream.concat(orderDebts.stream(), returnDebts.stream())
                // sort mới nhất/hoặc theo amount tuỳ bạn; ở đây sort amount desc cho dễ nhìn
                .sorted(Comparator.comparing(StoreOrderDebtItemResponse::getDebtNeedToPay,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // ================= ORDER -> DTO =================
    private StoreOrderDebtItemResponse toDebtItem(StoreOrder o) {

        BigDecimal real = nz(o.getShippingFeeReal());
        BigDecimal est  = nz(o.getShippingFee());

        BigDecimal afterSubtract = null;
        BigDecimal boomFee = null;

        boolean isBoomLike =
                Boolean.TRUE.equals(o.getReturnChargeApplied())
                        || o.getStatus() == OrderStatus.RETURNING
                        || o.getStatus() == OrderStatus.RETURNED;

        if (o.getStatus() == OrderStatus.DELIVERY_SUCCESS) {
            // sau khi trừ: max(real - estimate, 0)
            afterSubtract = real.subtract(est).max(BigDecimal.ZERO);
        }

        if (isBoomLike) {
            boomFee = real.multiply(new BigDecimal("1.5"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return StoreOrderDebtItemResponse.builder()
                .debtType("ORDER")

                .orderId(o.getId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus())

                .debtNeedToPay(nz(o.getTotalDebtOrder()))
                .shippingFeeCustomerPaid(est)
                .shippingFeeReal(real)
                .afterSubtract(afterSubtract)
                .boomFee(boomFee)
                .returnChargeApplied(Boolean.TRUE.equals(o.getReturnChargeApplied()))

                // return part null
                .returnRequestId(null)
                .ghnOrderCode(null)
                .build();
    }

    // ================= RETURN SHIPPING FEE -> DTO =================
    private StoreOrderDebtItemResponse toReturnDebtItem(ReturnShippingFee f) {

        // amount ưu tiên chargedToShop nếu có, không thì shippingFee
        BigDecimal charged = nz(f.getChargedToShop());
        BigDecimal amount = charged.compareTo(BigDecimal.ZERO) > 0 ? charged : nz(f.getShippingFee());

        return StoreOrderDebtItemResponse.builder()
                .debtType("RETURN_SHIP")

                // order part null
                .orderId(null)
                .orderCode(null)
                .status(null)

                // return part
                .returnRequestId(f.getReturnRequestId())
                .ghnOrderCode(f.getGhnOrderCode())

                .debtNeedToPay(amount)
                .shippingFeeCustomerPaid(BigDecimal.ZERO)
                .shippingFeeReal(nz(f.getShippingFee()))
                .afterSubtract(null)
                .boomFee(null)
                .returnChargeApplied(null)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
