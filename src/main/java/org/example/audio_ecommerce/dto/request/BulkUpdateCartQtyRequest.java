package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkUpdateCartQtyRequest {
    private List<Line> lines;

    @Data
    public static class Line {
        private String type;   // PRODUCT | COMBO (match CartItemType)
        private String refId;  // UUID string của product/combo
        private Integer quantity; // >=1
    }
}