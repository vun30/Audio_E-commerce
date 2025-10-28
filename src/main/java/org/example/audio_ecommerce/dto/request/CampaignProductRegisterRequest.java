// org.example.audio_ecommerce.dto.request.CampaignProductRegisterRequest
package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.VoucherType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignProductRegisterRequest {
    // Nếu campaign là FAST_SALE: yêu cầu slotId (có thể set ở ProductItem)
    private List<ProductItem> products;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductItem {
        private UUID productId;
        private UUID slotId;              // bắt buộc nếu FAST_SALE, null nếu MEGA_SALE

        // 🔹 Cấu hình giảm giá
        private VoucherType type;         // FIXED / PERCENT / SHIPPING
        private BigDecimal discountValue; // nếu FIXED
        private Integer discountPercent;  // nếu PERCENT
        private BigDecimal maxDiscountValue;
        private BigDecimal minOrderValue;

        // 🔹 Hạn mức phát hành
        private Integer totalVoucherIssued;
        private Integer totalUsageLimit;
        private Integer usagePerUser;
    }
}
