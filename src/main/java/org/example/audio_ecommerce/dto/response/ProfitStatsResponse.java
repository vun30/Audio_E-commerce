package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProfitStatsResponse {
    private BigDecimal totalRevenue;      // Tổng doanh thu (sum grandTotal của đơn hoàn tất)
    private BigDecimal totalCost;         // Tổng giá vốn (sum costPrice * quantity)
    private BigDecimal grossProfit;       // Lợi nhuận gộp = totalRevenue - totalCost
    private BigDecimal platformFee;       // Tổng phí nền tảng (sum platform_fee_amount)
    private BigDecimal netProfit;         // Lợi nhuận ròng = grossProfit - platformFee
}

