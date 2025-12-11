package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer sortOrder;
    private UUID parentId;
}