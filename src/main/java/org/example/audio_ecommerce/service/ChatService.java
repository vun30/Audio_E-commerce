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
    
    // New methods for customer-admin chat
    ChatMessageResponse sendAdminMessage(UUID customerId, UUID adminId, ChatMessageRequest req);
    List<ChatMessageResponse> getAdminMessages(UUID customerId, UUID adminId, int limit, String viewerType);
    List<ChatConversationResponse> getCustomerAdminConversations(UUID customerId);
    List<ChatConversationResponse> getAdminConversations(UUID adminId);
    void markAdminMessagesAsRead(UUID customerId, UUID adminId, String viewerId);
    void deleteAdminMessage(UUID customerId, UUID adminId, String messageId, String viewerType);
    void deleteAllAdminMessages(UUID customerId, UUID adminId, String viewerType);
    
    // New methods for store-admin chat
    ChatMessageResponse sendStoreAdminMessage(UUID storeId, UUID adminId, ChatMessageRequest req);
    List<ChatMessageResponse> getStoreAdminMessages(UUID storeId, UUID adminId, int limit, String viewerType);
    List<ChatConversationResponse> getStoreAdminConversations(UUID storeId);
    List<ChatConversationResponse> getAdminStoreConversations(UUID adminId);
    void markStoreAdminMessagesAsRead(UUID storeId, UUID adminId, String viewerId);
    void deleteStoreAdminMessage(UUID storeId, UUID adminId, String messageId, String viewerType);
    void deleteAllStoreAdminMessages(UUID storeId, UUID adminId, String viewerType);
}