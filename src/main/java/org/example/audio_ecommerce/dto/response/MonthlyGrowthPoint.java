package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGrowthPoint {

    private int month;                 // Tháng 1 → 12
    private long orders;               // Số đơn giao thành công

    private BigDecimal revenue;        // Doanh thu tháng

    private long returnSuccess;        // Số đơn return completed
    private double returnRate;         // % return

    private BigDecimal shippingDifference; // phí ship chênh lệch GHN
    private BigDecimal returnShippingFee;  // phí ship trả hàng
    private BigDecimal shippingCollected;   // phí ship thực GHN thu về
}
