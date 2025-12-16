package org.example.audio_ecommerce.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDebtReportResponse {
    private UUID storeId;
    private String storeName;
    private String currency; // "VND"
    private OffsetDateTime evaluatedAt;

    private Summary summary;

    private List<OrderDebtItem> orders;
    private List<ReturnFeeItem> returnShippingFees;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalDebt;
        private BigDecimal debtFromOrders;
        private BigDecimal debtFromReturnFees;
        private int debtOrdersCount;
        private int returnFeesCount;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderDebtItem {
        private UUID storeOrderId;
        private String orderCode;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime deliveredAt;
        private Boolean paidByShop;

        private Shipping shipping;
        private DebtDetail debtDetail;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Shipping {
        private BigDecimal shippingFeeEstimated;
        private BigDecimal shippingFeeReal;

        private Boolean returnChargeApplied;
        private BigDecimal returnChargeRate;       // 50.0
        private BigDecimal returnShippingCharge;   // 50% * real (nếu applied)

        private BigDecimal shippingExtraForStore;  // nếu deliveredAt != null: max(0, real-est)
        private String ruleApplied;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DebtDetail {
        private BigDecimal baseDebt;        // phần nợ “gốc”
        private BigDecimal returnDebt;      // phí quay đầu (nếu có)
        private BigDecimal totalDebtOrder;  // tổng nợ order (đúng logic bạn muốn)
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReturnFeeItem {
        private UUID id;
        private UUID returnRequestId;
        private String payer;           // "SHOP"
        private Boolean paidByShop;
        private String ghnOrderCode;

        private BigDecimal shippingFee;
        private BigDecimal chargedToShop;

        private String ruleApplied;
    }
}
