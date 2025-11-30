package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductViewAnalyticsResponse {
    private UUID productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private long viewCount;         // Tổng lượt xem
    private long orderCount;        // Số đơn hàng chứa sản phẩm này
    private double conversionRate;  // Tỉ lệ chuyển đổi = orderCount / viewCount * 100
}

