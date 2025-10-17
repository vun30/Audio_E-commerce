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
    private BigDecimal minOrderValue;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
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
                .minOrderValue(v.getMinOrderValue())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .startTime(v.getStartTime())
                .endTime(v.getEndTime())
                .products(v.getVoucherProducts().stream()
                        .map(p -> new ProductInfo(
                                p.getProduct().getProductId(),
                                p.getProduct().getName(),
                                p.getOriginalPrice(),
                                p.getDiscountedPrice(),
                                p.getDiscountPercent()
                        ))
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID productId;
        private String productName;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private Integer discountPercent;
    }
}
