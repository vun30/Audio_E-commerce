package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtAmountBreakdownDto {
    private BigDecimal total;
    private BigDecimal paid;
    private BigDecimal outstanding;
}
