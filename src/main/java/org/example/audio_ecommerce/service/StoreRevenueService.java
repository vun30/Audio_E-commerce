package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.StoreTotalRevenueResponse;
import org.example.audio_ecommerce.dto.response.StoreRevenueDetailResponse;

import java.util.UUID;

public interface StoreRevenueService {
    StoreTotalRevenueResponse getStoreTotalRevenue(UUID storeId);
}