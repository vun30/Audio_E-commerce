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

    private String lastMessage;
    private Instant lastMessageTime;
}
