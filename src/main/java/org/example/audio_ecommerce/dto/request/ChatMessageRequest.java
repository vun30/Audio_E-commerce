package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ChatMessageType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ChatMessageRequest {
    private String senderId;      // UUID string
    private String senderType;    // "CUSTOMER" hoặc "STORE"

    // text
    private String content;       // nếu là TEXT thì dùng content

    // media
    @NotNull
    private ChatMessageType messageType;   // "TEXT", "IMAGE", "VIDEO"

    private String mediaUrl;      // URL hình/video (Cloudinary)

    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
}
