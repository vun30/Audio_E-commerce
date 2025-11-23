package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductReviewCreateRequest {

    @NotNull
    private UUID customerOrderItemId;

    @Min(1) @Max(5)
    private int rating;

    private String content;

    private List<ReviewMediaRequest> media; // optional

    @Data
    public static class ReviewMediaRequest {
        @NotNull
        private String type; // IMAGE / VIDEO
        @NotNull
        private String url;
    }
}
