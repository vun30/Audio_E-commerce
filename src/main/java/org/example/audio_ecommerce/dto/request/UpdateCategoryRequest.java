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
public class UpdateCategoryRequest {

    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer sortOrder;
    private UUID parentId;

    private List<AttributeToAdd> attributesToAdd;
    private List<AttributeToUpdate> attributesToUpdate;
    private List<UUID> attributesToDelete;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttributeToAdd {
        private String attributeName;
        private String attributeLabel;
        private CategoryAttributeDataType dataType;
        private List<String> options;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttributeToUpdate {
        private UUID attributeId;
        private String attributeName;
        private String attributeLabel;
        private CategoryAttributeDataType dataType;
        private List<String> options;
    }
}
