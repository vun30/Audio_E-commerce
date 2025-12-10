package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCategoryRequest {

    private String name;
    private UUID parentId;

    private List<AttributeToAdd> attributesToAdd;
    private List<AttributeToUpdate> attributesToUpdate;
    private List<UUID> attributesToDelete;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeToAdd {
        private String attributeName;
        private String attributeLabel;
        private String dataType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeToUpdate {
        private UUID attributeId;
        private String attributeName;
        private String attributeLabel;
        private String dataType;
    }
}
