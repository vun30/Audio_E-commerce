package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
public class StoreNotificationActionRequest {
    private String action;                 // "OPEN", "ACCEPT", "REJECT", ...
    private Map<String, Object> extra;     // tuỳ anh cần gì
}
