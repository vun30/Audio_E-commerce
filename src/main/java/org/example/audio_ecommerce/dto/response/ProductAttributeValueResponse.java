package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for returning product attribute values to client
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValueResponse {

    @Schema(
            description = "ID of the attribute value",
            example = "550e8400-e29b-41d4-a716-446655440001"
    )
    private UUID id;

    @Schema(
            description = "ID of the attribute (CategoryAttribute)",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID attributeId;

    @Schema(
            description = "Code name of the attribute",
            example = "frequencyResponse"
    )
    private String attributeName;

    @Schema(
            description = "Display label of the attribute",
            example = "Frequency Response"
    )
    private String attributeLabel;

    @Schema(
            description = "Data type: STRING, NUMBER, BOOLEAN, ENUM, JSON",
            example = "STRING"
    )
    private String dataType;

    @Schema(
            description = "Attribute value",
            example = "20Hz - 20000Hz"
    )
    private String value;
}

