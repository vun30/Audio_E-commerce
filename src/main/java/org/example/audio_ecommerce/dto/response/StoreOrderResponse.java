package org.example.audio_ecommerce.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class StoreOrderResponse {
    private UUID id;
    private UUID storeId;
    private String status;
    // Thêm các trường khác nếu cần
}

