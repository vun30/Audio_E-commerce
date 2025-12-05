package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopReturnProduct {
    private UUID productId;
    private String productName;
    private long returnCount;
}
