package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGhnOrderRequest {
    private UUID storeOrderId;           // id đơn hàng của store
    private UUID storeId;                // id cửa hàng
    private String orderGhn;             // mã đơn GHN (order_code)
    private BigDecimal totalFee;         // tổng phí GHN
    private LocalDateTime expectedDeliveryTime; // thời gian giao dự kiến
    private String status;               // trạng thái GHN (ready_to_pick, delivering, delivered,...)
}
