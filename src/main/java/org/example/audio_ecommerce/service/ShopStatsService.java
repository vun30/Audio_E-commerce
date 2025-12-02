package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.ShopOverviewStatsResponse;
import org.example.audio_ecommerce.dto.response.OrderStatusCountsResponse;
import org.example.audio_ecommerce.dto.response.OrdersByDayResponse;
import org.example.audio_ecommerce.dto.response.OrderStatusRatioResponse;
import org.example.audio_ecommerce.dto.response.ProfitStatsResponse;
import org.example.audio_ecommerce.dto.response.TopSellingProductResponse;
import org.example.audio_ecommerce.dto.response.ProductViewAnalyticsResponse;
import org.example.audio_ecommerce.dto.response.OutOfStockProductResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface ShopStatsService {
    ShopOverviewStatsResponse getOverview(UUID storeId, LocalDate date, LocalDate startDate, LocalDate endDate);
    OrderStatusCountsResponse getOrderStatusCounts(UUID storeId, LocalDate fromDate, LocalDate toDate);
    OrdersByDayResponse getOrdersByDay(UUID storeId, LocalDate fromDate, LocalDate toDate);
    OrderStatusRatioResponse getOrderStatusRatio(UUID storeId, LocalDate fromDate, LocalDate toDate);
    ProfitStatsResponse getProfitStats(UUID storeId, LocalDate fromDate, LocalDate toDate);

    // Product Analytics
    java.util.List<TopSellingProductResponse> getTopSellingProducts(UUID storeId, LocalDate fromDate, LocalDate toDate, int limit);
    java.util.List<ProductViewAnalyticsResponse> getProductViewAnalytics(UUID storeId);
    java.util.List<OutOfStockProductResponse> getOutOfStockProducts(UUID storeId, Integer threshold);
}
