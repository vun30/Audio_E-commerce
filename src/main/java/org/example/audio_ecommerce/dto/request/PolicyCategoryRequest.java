package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyCategoryRequest {


    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String description;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean isActive;
}


