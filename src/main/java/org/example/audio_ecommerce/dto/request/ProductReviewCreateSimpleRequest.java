package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductReviewCreateSimpleRequest {
    private UUID orderId;
    private UUID productId;
    private int rating;
    private String content;
    private List<MediaRequest> media;

    @Data
    public static class MediaRequest {
        private String type;
        private String url;
    }
}
