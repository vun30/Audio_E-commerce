package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboItemRequest {

    private UUID productId;

    private UUID variantId;

    private String optionName;     // Color, Size,...
    private String optionValue;    // Black, M,...

    private BigDecimal variantPrice;

    private Integer variantStock;

    private String variantUrl;

    private String variantSku;

    private Integer quantity; // >= 1
}
