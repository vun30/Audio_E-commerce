package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class PreviewCampaignPriceRequest {
    private UUID customerId; // FE truyền thẳng vào
    private UUID variantId;
    private Integer quantity;
}
