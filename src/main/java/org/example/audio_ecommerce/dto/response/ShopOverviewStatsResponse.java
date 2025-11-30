package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopOverviewStatsResponse {
    // Today metrics
    private long totalOrdersToday;
    private BigDecimal revenueToday;

    // Lifetime / filtered totals
    private long totalOrdersSuccess;
    private long totalOrdersCancelled;

    // This month
    private BigDecimal revenueThisMonth;
    private BigDecimal profitThisMonth;

    // Conversion rate (orders / views)
    private double conversionRate;

    // Views
    private long totalProductViews;

    // Inventory summary
    private long totalProductsInStock;
    private long totalProductsOutOfStock;
}
