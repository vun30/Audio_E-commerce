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
        @NotNull private UUID id;
        @Min(1) private int quantity;
    }
}
