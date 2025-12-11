package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderItemResponse {
    private UUID id;
    private String type;
    private UUID refId;
    private String name;
    private int quantity;

    // Snapshot pricing
    private BigDecimal unitPriceBeforeDiscount;
    private BigDecimal linePriceBeforeDiscount;
    private BigDecimal platformVoucherDiscount;
    private BigDecimal shopItemDiscount;
    private BigDecimal shopOrderVoucherDiscount;
    private BigDecimal totalItemDiscount;
    private BigDecimal finalUnitPrice;
    private BigDecimal finalLineTotal;
    private BigDecimal amountCharged;

    // Legacy fields kept for compatibility
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    private Boolean eligibleForPayout;
    private Boolean isPayout;
    private Boolean isReturned;

    private BigDecimal shippingFeeEstimated;
    private BigDecimal shippingFeeActual;
    private BigDecimal shippingExtraForStore;

    private BigDecimal platformFeeAmount;
    private BigDecimal netPayoutItem;

    private Boolean payoutProcessed;

    private BigDecimal platformFeePercentage;
}