package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.StoreDashboardResponses;

import java.time.LocalDateTime;

public interface StoreDashboardService {

    StoreDashboardResponses.StoreDashboardSummaryResponse getSummary(LocalDateTime from, LocalDateTime to);

    StoreDashboardResponses.StoreReturnStatsResponse getReturnStats(LocalDateTime from, LocalDateTime to);

    StoreDashboardResponses.GrowthResponse growthByMonth(int year);

    StoreDashboardResponses.GrowthResponse growthByYear(int fromYear, int toYear);

    // 1 call trả tất cả (summary + returns + growth theo month của year)
    StoreDashboardResponses.StoreDashboardFullResponse getFull(LocalDateTime from, LocalDateTime to, Integer year);
}