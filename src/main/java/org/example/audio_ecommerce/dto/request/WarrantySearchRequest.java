package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class WarrantySearchRequest {
    private String serial;
    private UUID orderId;     // CustomerOrder Id
    private String phoneOrEmail;
}
