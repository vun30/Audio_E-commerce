package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface RevenueService {

    // === Ghi nhận doanh thu (call từ luồng Wallet / Scheduler 7 ngày) ===

    void recordStoreRevenue(
            UUID storeId,
            UUID storeOrderId,
            BigDecimal amount,
            BigDecimal feePlatform,
            BigDecimal feeShipping,
            LocalDate revenueDate
    );

    void recordPlatformRevenue(
            UUID storeOrderId,
            PlatformRevenueType type,
            BigDecimal amount,
            LocalDate revenueDate
    );

    // === Query doanh thu cho shop ===
    Page<StoreRevenueResponse> getStoreRevenue(
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    StoreRevenueSummaryResponse getStoreRevenueSummary(
            UUID storeId,
            LocalDate fromDate,
            LocalDate toDate
    );

    // === Query doanh thu nền tảng ===
    Page<PlatformRevenueResponse> getPlatformRevenue(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    PlatformRevenueSummaryResponse getPlatformRevenueSummary(
            LocalDate fromDate,
            LocalDate toDate
    );
}
