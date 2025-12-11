package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderReportEntry {
    private UUID storeOrderId;
    private String orderCode;
    private UUID storeId;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private BigDecimal productsTotal;
    private BigDecimal customerShippingFee; // so.shippingFee
    private BigDecimal actualShippingFee;   // so.actualShippingFee
    private BigDecimal shippingExtraForStore;
    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount;   // aggregated per storeOrder
    private BigDecimal netPayoutToStore;
    private List<ItemContribution> items;  // items that caused this entry (filtered)
}
