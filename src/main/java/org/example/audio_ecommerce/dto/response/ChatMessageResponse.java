package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String senderId;
    private String senderType;

    private String content;

    private String messageType;   // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;      // nếu là media thì frontend render chỗ này

    private Instant createdAt;
    private Boolean read;
}
