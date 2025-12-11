package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private UUID categoryId;
    private String name;
    private UUID parentId;

    private List<AttributeResponse> attributes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttributeResponse {
        private UUID attributeId;
        private String attributeName;
        private String attributeLabel;
        private CategoryAttributeDataType dataType;
        private List<String> options;   // MUST NOT BE '?'
    }
}
