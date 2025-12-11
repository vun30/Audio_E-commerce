package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;

@Data
public class UpdateStoreStatusRequest {
    private StoreStatus status;
    private String reason; // có thể null nếu ACTIVE, PAUSED
}
