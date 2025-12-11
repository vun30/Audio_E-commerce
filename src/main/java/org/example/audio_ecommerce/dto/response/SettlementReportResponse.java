package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.SettlementReportType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementReportResponse {
    private SettlementReportType reportType;
    private LocalDate date; // if provided
    private List<StoreOrderReportEntry> entries;
    private BigDecimal totalAmount; // sum across entries (meaning depends on type)
}
