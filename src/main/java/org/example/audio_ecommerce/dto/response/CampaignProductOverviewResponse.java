package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProductOverviewResponse {

    private UUID campaignId;
    private String campaignName;
    private String campaignType;
    private List<ProductDto> products;

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ProductDto {
        private UUID campaignProductId; // ✅ ID bảng trung gian
        private UUID productId;
        private String productName;
        private String productImage;
        private BigDecimal originalPrice;
        private UUID storeId;
        private String storeName;
        private VoucherDto voucher;
        private List<FlashSlotDto> flashSaleSlots;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class VoucherDto {
        private String type;
        private BigDecimal discountValue;
        private Integer discountPercent;
        private BigDecimal maxDiscountValue;
        private BigDecimal minOrderValue;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class FlashSlotDto {
        private UUID slotId;
        private LocalDateTime openTime;
        private LocalDateTime closeTime;
        private String status;
        private VoucherDto voucher;
    }
}
