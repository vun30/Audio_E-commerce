package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {

    private String name;
    private UUID parentId;

    private List<AttributeRequest> attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeRequest {
        private String attributeName;
        private String attributeLabel;
        private String dataType;   // STRING / NUMBER / BOOLEAN / ENUM
    }
}
