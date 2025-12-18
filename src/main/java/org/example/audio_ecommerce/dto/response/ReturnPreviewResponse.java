package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReturnPreviewResponse {
    private UUID orderId;
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private UUID orderItemId;
        private UUID productId;
        private String productName;
        private Integer quantity;

        // số tiền hoàn cho item (không gồm ship)
        private BigDecimal refundableAmount;

        // optional: debug/breakdown để FE hiển thị rõ
        private BigDecimal finalLineTotal;
        private BigDecimal amountCharged;
    }
}
