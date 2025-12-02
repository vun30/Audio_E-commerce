package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TopSellingProductResponse {
    private UUID productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private long totalSold;        // Tổng số lượng đã bán
    private BigDecimal totalRevenue; // Tổng doanh thu từ sản phẩm này
    private Integer stockQuantity;  // Tồn kho hiện tại
}

