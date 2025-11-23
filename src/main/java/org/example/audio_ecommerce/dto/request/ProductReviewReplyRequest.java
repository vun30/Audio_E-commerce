package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductReviewReplyRequest {

    @NotBlank
    private String content;
}
