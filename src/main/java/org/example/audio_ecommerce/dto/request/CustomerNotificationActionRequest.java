package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class CustomerNotificationActionRequest {
    private String action; // ví dụ: OPEN, GO_TO_ORDER, ...
}
