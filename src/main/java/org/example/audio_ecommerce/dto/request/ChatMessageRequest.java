package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String senderId;      // UUID string
    private String senderType;    // "CUSTOMER" hoặc "STORE"

    // text
    private String content;       // nếu là TEXT thì dùng content

    // media
    private String messageType;   // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;      // URL hình/video (Cloudinary)
}
