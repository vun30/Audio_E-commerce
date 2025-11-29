// AddCartItemsRequest.java
package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCartItemsRequest {
    @NotNull
    private List<CartItemLine> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemLine {
        @NotNull private String type;   // "PRODUCT" | "COMBO"
        // PRODUCT: dùng productId / variantId (1 trong 2 có thể null)
        private UUID productId;    // null nếu chỉ gửi variantId
        private UUID variantId;    // null nếu product không có variant

        // COMBO: dùng comboId
        private UUID comboId;
        @Min(1) private int quantity;
    }
}
