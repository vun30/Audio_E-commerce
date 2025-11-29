package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class ProductReviewUpdateRequest {

    @Min(1) @Max(5)
    private int rating;

    private String content;

    private List<ProductReviewCreateRequest.ReviewMediaRequest> media;
}
