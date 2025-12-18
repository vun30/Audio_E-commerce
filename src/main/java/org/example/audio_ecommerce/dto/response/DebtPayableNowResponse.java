package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DebtPayableNowResponse {
    private BigDecimal totalPayableNow;
    private Page<DebtComponentItemResponse> page; // hoặc List<DebtComponentItemResponse> nếu bạn không cần Page
}
