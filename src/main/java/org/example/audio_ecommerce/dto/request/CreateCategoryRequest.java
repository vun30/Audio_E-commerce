package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {

    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer sortOrder;
    private UUID parentId;

    private List<AttributeRequest> attributes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttributeRequest {
        private String attributeName;          // e.g., "maxSPL"
        private String attributeLabel;         // e.g., "Mức SPL"
        private CategoryAttributeDataType dataType;
        private List<String> options;          // null nếu không phải SELECT/MULTI_SELECT
    }
}
