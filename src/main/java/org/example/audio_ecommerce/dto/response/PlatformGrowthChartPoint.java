package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformGrowthChartPoint {

    // Thời gian
    private int year;
    private int month; // nếu chart theo năm thì month = 0

    // Line 1: Doanh thu nền tảng (phí nền tảng)
    private BigDecimal platformRevenue;

    // Line 2: Tỷ lệ return (%)
    private BigDecimal returnRate;
}
