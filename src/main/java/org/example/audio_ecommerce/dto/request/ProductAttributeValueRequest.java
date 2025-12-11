package org.example.audio_ecommerce.dto.request;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValueRequest {

    @Schema(description = "ID thuộc tính category", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID attributeId;

    @Schema(
            description = "Giá trị thuộc tính",
            example = "20Hz - 20000Hz (STRING), 50 (NUMBER), true (BOOLEAN), [\"A\",\"B\"] (JSON)"
    )
    private String value;
}
