// service/ChatService.java
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ChatMessageRequest;
import org.example.audio_ecommerce.dto.response.ChatConversationResponse;
import org.example.audio_ecommerce.dto.response.ChatMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    ChatMessageResponse sendMessage(UUID customerId, UUID storeId, ChatMessageRequest req);
    List<ChatMessageResponse> getMessages(UUID customerId, UUID storeId, int limit, String viewerType);
    List<ChatConversationResponse> getCustomerConversations(UUID customerId);
    List<ChatConversationResponse> getStoreConversations(UUID storeId);
    void markMessagesAsRead(UUID customerId, UUID storeId, String viewerId);
    void deleteMessage(UUID customerId, UUID storeId, String messageId, String viewerType);
    void deleteAllMessages(UUID customerId, UUID storeId, String viewerType);
}
