package org.example.audio_ecommerce.service;



import org.example.audio_ecommerce.dto.response.StoreDebtReportResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StoreReportService {

    /**
     * Báo cáo nợ của store: tổng nợ + chi tiết theo từng StoreOrder + ReturnShippingFee
     * @param storeId store cần xem report
     * @param from optional: lọc theo createdAt >= from (null = không lọc)
     * @param to   optional: lọc theo createdAt <= to (null = không lọc)
     */
    StoreDebtReportResponse getDebtReport(UUID storeId, LocalDateTime from, LocalDateTime to);
}
