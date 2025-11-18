package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformRevenueResponse {

    private UUID id;
    private UUID storeOrderId;

    private PlatformRevenueType type;
    private BigDecimal amount;

    private LocalDate revenueDate;
    private LocalDateTime createdAt;
}
