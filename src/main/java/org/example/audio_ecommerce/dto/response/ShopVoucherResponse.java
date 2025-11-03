package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.ShopVoucher;
import org.example.audio_ecommerce.entity.ShopVoucherProduct;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopVoucherResponse {

    private UUID id;
    private String code;
    private String title;
    private String description;
    private String type;
    private BigDecimal discountValue;
    private Integer discountPercent;
    private BigDecimal maxDiscountValue;
    private BigDecimal minOrderValue;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalVoucherIssued;
    private Integer usagePerUser;
    private Integer remainingUsage;

    private List<ProductInfo> products;

    public static ShopVoucherResponse fromEntity(ShopVoucher v) {
        return ShopVoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .title(v.getTitle())
                .description(v.getDescription())
                .type(v.getType() != null ? v.getType().name() : null)
                .discountValue(v.getDiscountValue())
                .discountPercent(v.getDiscountPercent())
                .maxDiscountValue(v.getMaxDiscountValue())
                .minOrderValue(v.getMinOrderValue())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .startTime(v.getStartTime())
                .endTime(v.getEndTime())
                .totalVoucherIssued(v.getTotalVoucherIssued())
                .usagePerUser(v.getUsagePerUser())
                .remainingUsage(v.getRemainingUsage())
                .products(
                        v.getVoucherProducts() != null
                                ? v.getVoucherProducts().stream()
                                .map(ShopVoucherResponse.ProductInfo::fromEntity)
                                .collect(Collectors.toList())
                                : null)
                .build();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductInfo {
        private UUID productId;
        private String productName;
        private Integer promotionStockLimit;
        private Integer purchaseLimitPerCustomer;
        private boolean active;

        public static ProductInfo fromEntity(ShopVoucherProduct p) {
            return ProductInfo.builder()
                    .productId(p.getProduct().getProductId())
                    .productName(p.getProduct().getName())
                    .promotionStockLimit(p.getPromotionStockLimit())
                    .purchaseLimitPerCustomer(p.getPurchaseLimitPerCustomer())
                    .active(p.isActive())
                    .build();
        }
    }
}
