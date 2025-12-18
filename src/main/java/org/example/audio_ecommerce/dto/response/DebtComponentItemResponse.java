package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.DebtComponentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DebtComponentItemResponse {
    private DebtComponentType componentType; // SHIP_DIFF | RTO_FEE | RETURN_SHIPPING_FEE
    private String displayType;

    private UUID refId;          // store_order.id hoặc return_shipping_fees.return_request_id (hoặc id nếu có)
    private String orderCode;    // store_order.order_code (nếu có)
    private String ghnOrderCode; // return_shipping_fees.ghn_order_code (nếu có)

    private BigDecimal amount;   // số tiền của component
    private String status;       // UNPAID | PAID
    private LocalDateTime occurredAt; // createdAt hoặc returnChargeAppliedAt hoặc BaseEntity.createdAt
    private String description;  // giải thích cách tính
}
