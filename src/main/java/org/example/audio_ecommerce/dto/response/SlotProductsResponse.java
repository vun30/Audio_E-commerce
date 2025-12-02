// org.example.audio_ecommerce.dto.response.SlotProductsResponse
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlotProductsResponse {
    private UUID campaignId;
    private UUID slotId; // null nếu mega sale
    private String timeFilter; // EXPIRED / ONGOING / UPCOMING
    private List<Item> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private UUID campaignProductId;
        private UUID productId;
        private String productName;
        private String brandName;

        private VoucherType type;
        private BigDecimal discountValue;
        private Integer discountPercent;
        private BigDecimal maxDiscountValue;
        private BigDecimal minOrderValue;

        private Integer totalVoucherIssued;
        private Integer totalUsageLimit;
        private Integer usagePerUser;
        private Integer remainingUsage;

        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private VoucherStatus status;
    }
}
