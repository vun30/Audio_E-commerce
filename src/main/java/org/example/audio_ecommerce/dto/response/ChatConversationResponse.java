package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ChatConversationResponse {
    private String id;              // conversationId: customerId_storeId
    private UUID customerId;
    private UUID storeId;
    private UUID adminId;           // for admin conversations

    private String lastMessage;
    private Instant lastMessageTime;

    private long customerUnreadCount;
    private long storeUnreadCount;
    private long adminUnreadCount;  // for admin conversations
}