package org.example.audio_ecommerce.dto.request;

import lombok.Data;
@Data
public class WarrantyActivateSerialRequest {
    private String serialNumber;
    private String note;
}
