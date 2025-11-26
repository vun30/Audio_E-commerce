package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String senderId;      // UUID string
    private String senderType;    // "CUSTOMER" hoặc "STORE"
    private String content;
}
