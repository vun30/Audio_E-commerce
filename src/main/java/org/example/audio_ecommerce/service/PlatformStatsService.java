package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.PlatformUserStoreGrowthPoint;
import org.example.audio_ecommerce.dto.response.PlatformUserStoreOverviewResponse;

import java.util.List;

public interface PlatformStatsService {

    PlatformUserStoreOverviewResponse getUserStoreOverview(Integer year, Integer month);

    List<PlatformUserStoreGrowthPoint> getUserStoreGrowthChartByYear(Integer year);
}
