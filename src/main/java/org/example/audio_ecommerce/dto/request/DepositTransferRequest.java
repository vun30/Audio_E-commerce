package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DepositTransferRequest {
    private BigDecimal amount;     // số tiền muốn chuyển sang ví cọc
    private String note;           // optional
}
