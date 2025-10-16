package org.example.audio_ecommerce.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class CustomerOrderResponse {
    private UUID id;
    private String status;
    // Thêm các trường khác nếu cần
}

