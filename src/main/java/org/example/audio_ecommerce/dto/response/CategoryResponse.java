package org.example.audio_ecommerce.dto.response;

import lombok.*;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeResponse {
        private UUID attributeId;
        private String attributeName;
        private String attributeLabel;
        private String dataType;
    }
}
