package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopVoucherAppliedResponse {
    private UUID voucherId;        // nếu json có
    private String code;           // nếu json có
    private String name;           // nếu json có
    private BigDecimal discount;   // số tiền giảm của shop voucher (order-level)
}
