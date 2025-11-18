package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformRevenueSummaryResponse {

    private LocalDate fromDate;
    private LocalDate toDate;

    // Tổng theo từng loại
    private Map<PlatformRevenueType, BigDecimal> totalByType;

    // Tổng tất cả loại
    private BigDecimal totalAll;
}
