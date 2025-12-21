package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class StoreCancelOrderRequest {
    private String reason; // optional
}
