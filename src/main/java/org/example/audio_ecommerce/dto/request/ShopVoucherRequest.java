package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopVoucherRequest {

    // ========== 🔹 Thông tin cơ bản ==========
    @io.swagger.v3.oas.annotations.media.Schema(description = "Mã voucher (optional). Nếu bỏ trống hệ thống sẽ tự sinh 4 ký tự ngẫu nhiên", example = "SALE10K")
    private String code;
    private String title;
    private String description;
    private VoucherType type;

    private BigDecimal discountValue;
    private Integer discountPercent;
    private BigDecimal maxDiscountValue;
    private BigDecimal minOrderValue;

    // ========== 🔹 Hạn mức ==========
    private Integer totalVoucherIssued;
    private Integer usagePerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ========== 🔹 Danh sách sản phẩm áp dụng ==========
    private List<VoucherProductItem> products;

    private ShopVoucherScopeType scopeType; // PRODUCT_VOUCHER hoặc ALL_SHOP_VOUCHER

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherProductItem {
        private UUID productId;
        private Integer discountPercent;
        private BigDecimal discountAmount;
        private Integer promotionStockLimit;
        private Integer purchaseLimitPerCustomer;
    }
}
