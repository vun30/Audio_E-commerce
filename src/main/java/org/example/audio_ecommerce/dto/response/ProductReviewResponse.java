package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductReviewResponse {

    private UUID id;
    private int rating;
    private String content;
    private LocalDateTime createdAt;

    // customer info
    private UUID customerId;
    private String customerName;
    private String customerAvatarUrl;
    private String status;
    // product info
    private UUID productId;

    // variant
    private String variantOptionName;
    private String variantOptionValue;

    private List<ReviewMediaResponse> media;
    private List<ReviewReplyResponse> replies;

    @Data
    @Builder
    public static class ReviewMediaResponse {
        private String type;
        private String url;
    }

    @Data
    @Builder
    public static class ReviewReplyResponse {
        private String storeName;
        private String content;
        private LocalDateTime createdAt;
    }
}
