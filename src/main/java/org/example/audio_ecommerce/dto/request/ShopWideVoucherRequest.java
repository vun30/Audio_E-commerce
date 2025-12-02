package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherType;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopWideVoucherRequest {
    // ========== Thông tin cơ bản ==========
    private String code;
    private String title;
    private String description;
    private VoucherType type;
    private BigDecimal discountValue;
    private Integer discountPercent;
    private BigDecimal maxDiscountValue;
    private BigDecimal minOrderValue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ========== Thông tin bổ sung ==========
    private Integer totalVoucherIssued;   // Số lượng phát hành
    private Integer usagePerUser;         // Mỗi user dùng tối đa
    private Integer remainingUsage;       // Số lượt còn lại
    private ShopVoucherScopeType scopeType; // PRODUCT_VOUCHER hoặc ALL_SHOP_VOUCHER
}
