package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.LifetimeStatsResponse;
import org.example.audio_ecommerce.dto.response.MonthlyGrowthPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShopStatsService {
    LifetimeStatsResponse getLifetimeStats(UUID storeId);
LifetimeStatsResponse getLifetimeStatsByRange(UUID storeId, LocalDate from, LocalDate to);

List<MonthlyGrowthPoint> getYearGrowth(UUID storeId, int year);

}
