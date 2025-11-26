package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShopVoucherUsageResponse {
    private UUID id;

    private UUID voucherId;
    private String voucherCode;
    private String voucherTitle;

    private UUID storeId;
    private String storeName;

    private UUID customerId;
    private String customerName;
    private String customerEmail;

    private Integer usedCount;
    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;
}
