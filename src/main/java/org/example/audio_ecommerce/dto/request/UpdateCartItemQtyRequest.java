package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCartItemQtyRequest {
    private UUID cartItemId;
    private Integer quantity; // >= 1
}