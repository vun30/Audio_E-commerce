// dto/response/ChatMessageResponse.java
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
    private Instant createdAt;
    private Boolean read;
}
