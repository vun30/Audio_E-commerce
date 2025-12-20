package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class StoreDashboardResponses {

    // =========================
    // 1) Summary
    // =========================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StoreDashboardSummaryResponse {
        private BigDecimal grossRevenue;       // SUM(finalLineTotal) payout items
        private BigDecimal platformFeePaid;    // SUM(platformFeeAmount) payout items
        private BigDecimal netRevenue;         // gross - fee

        private Long deliveredOrderCount;      // số đơn delivered trong range
        private Long itemsSold;                // SUM(quantity) payout items trong range

        // ✅ NEW: TOP 10 sản phẩm bán chạy nhất
        private List<StoreTopSellingResponse> top10Selling;

        // (OPTIONAL) giữ backward compatible nếu FE cũ đang dùng top1:
        // private UUID topSellingRefId;
        // private Long topSellingQuantity;
    }

    // Top selling item
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StoreTopSellingResponse {
        private UUID refId;
        private Long quantitySold;
    }

    // =========================
    // 2) Return stats
    // =========================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StoreReturnStatsResponse {
        private Long returnCount;                          // số return (lọc status)
        private List<ReturnedProductCount> top5ReturnedProducts; // top5 productId
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ReturnedProductCount {
        private UUID productId;
        private Long count;
    }

    // =========================
    // 3) Growth chart
    // =========================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GrowthPointResponse {
        private Integer year;
        private Integer month;                 // nullable nếu theo năm
        private BigDecimal grossRevenue;
        private BigDecimal platformFeePaid;
        private BigDecimal netRevenue;
        private Long deliveredOrderCount;
        private Long itemsSold;

        // ❌ KHÔNG để top10Selling ở đây (vì chart point không yêu cầu top list)
        // Nếu bạn muốn chart cũng có top10 theo từng tháng thì sẽ là một query khác.
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class GrowthResponse {
        private Integer year;
        private String granularity;            // "MONTH" hoặc "YEAR"
        private List<GrowthPointResponse> points;
    }

    // ===== FULL DASHBOARD RESPONSE =====
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StoreDashboardFullResponse {
        private StoreDashboardSummaryResponse summary;
        private StoreReturnStatsResponse returns;
        private GrowthResponse growth;
    }
}
