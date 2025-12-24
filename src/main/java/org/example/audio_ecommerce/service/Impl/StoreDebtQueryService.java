package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.StoreOrderDebtItemResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreDebtQueryService {

    private final StoreOrderRepository storeOrderRepository;

    // bạn chỉnh end status ở đây
    private static final EnumSet<OrderStatus> END_STATUSES = EnumSet.of(
            OrderStatus.DELIVERY_SUCCESS,
            OrderStatus.RETURNED,
            OrderStatus.RETURNING
    );

    @Transactional(readOnly = true)
    public List<StoreOrderDebtItemResponse> getUnpaidEndDebtOrders(UUID storeId) {

        List<StoreOrder> orders = storeOrderRepository.findUnpaidDebtEndOrdersByStore(
                storeId,
                List.copyOf(END_STATUSES)
        );

        return orders.stream().map(this::toDebtItem).toList();
    }

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
                .orderId(o.getId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus())
                .debtNeedToPay(nz(o.getTotalDebtOrder()))
                .shippingFeeCustomerPaid(est)
                .shippingFeeReal(real)
                .afterSubtract(afterSubtract)
                .boomFee(boomFee)
                .returnChargeApplied(Boolean.TRUE.equals(o.getReturnChargeApplied()))
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
