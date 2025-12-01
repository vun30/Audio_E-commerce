package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ChatMessageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String senderId;
    private String senderType;

    private String content;

    private ChatMessageType messageType;   // "TEXT", "IMAGE", "VIDEO"
    private List<MediaItem> mediaUrl;      // nếu là media thì frontend render chỗ này

    private Instant createdAt;
    private Boolean read;
    private Boolean deletedForCustomer;
    private Boolean deletedForStore;

    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;

    @Data
    @Builder
    public static class MediaItem {
        private String url;
        private String type;
    }
}
