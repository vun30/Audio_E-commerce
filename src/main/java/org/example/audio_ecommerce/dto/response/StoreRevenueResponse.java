package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRevenueResponse {

    private UUID id;
    private UUID storeId;
    private UUID storeOrderId;

    private BigDecimal amount;
    private BigDecimal feePlatform;
    private BigDecimal feeShipping;

    private LocalDate revenueDate;
    private LocalDateTime createdAt;
}
