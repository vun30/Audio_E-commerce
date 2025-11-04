package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignDeliveryRequest {
    private UUID deliveryStaffId;
    private UUID preparedByStaffId; // optional
    private String note;
}
