package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.PlatformPayoutRevenueStatsResponse;

import java.util.List;
import java.util.UUID;

public interface PlatformPayoutStatsService {

    PlatformPayoutRevenueStatsResponse getPayoutRevenueStats(
            Integer year,
            Integer month,
            List<UUID> storeIds
    );
}
