package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ChatMessageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String senderId;
    private String senderType;

    private String content;

    private ChatMessageType messageType;   // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;      // nếu là media thì frontend render chỗ này

    private Instant createdAt;
    private Boolean read;

    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
}
