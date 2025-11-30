package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OutOfStockProductResponse {
    private UUID productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private Integer stockQuantity;  // Số lượng tồn hiện tại
    private Integer threshold;      // Ngưỡng cảnh báo (mặc định < 10)
    private String status;          // OUT_OF_STOCK / LOW_STOCK
}

