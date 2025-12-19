// controller/ChatController.java
package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ChatMessageRequest;
import org.example.audio_ecommerce.dto.response.ChatConversationResponse;
import org.example.audio_ecommerce.dto.response.ChatMessageResponse;
import org.example.audio_ecommerce.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // gửi tin nhắn trong 1 cuộc chat giữa customer và store
    @PostMapping("/conversations/{customerId}/{storeId}/messages")
    public ChatMessageResponse sendMessage(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @Valid @RequestBody ChatMessageRequest req
    ) {
        return chatService.sendMessage(customerId, storeId, req);
    }

    // lấy lịch sử chat
    @GetMapping("/conversations/{customerId}/{storeId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam String viewerType
    ) {
        return chatService.getMessages(customerId, storeId, limit, viewerType);
    }

    // ============= NEW: tất cả conversations của 1 customer =============

    @GetMapping("/customers/{customerId}/conversations")
    public List<ChatConversationResponse> getCustomerConversations(
            @PathVariable UUID customerId
    ) {
        return chatService.getCustomerConversations(customerId);
    }

    // ============= NEW: tất cả conversations của 1 store =============

    @GetMapping("/stores/{storeId}/conversations")
    public List<ChatConversationResponse> getStoreConversations(
            @PathVariable UUID storeId
    ) {
        return chatService.getStoreConversations(storeId);
    }

    @PostMapping("/conversations/{customerId}/{storeId}/read")
    public void markRead(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @RequestParam String viewerId // id của người đang xem tin nhắn
    ) {
        chatService.markMessagesAsRead(customerId, storeId, viewerId);
    }

    @DeleteMapping("/conversations/{customerId}/{storeId}/messages/{messageId}")
    public void deleteMessage(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @PathVariable String messageId,
            @RequestParam String viewerType // "CUSTOMER" hoặc "STORE"
    ) {
        chatService.deleteMessage(customerId, storeId, messageId, viewerType);
    }

    @DeleteMapping("/conversations/{customerId}/{storeId}/messages")
    public void deleteAllMessages(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @RequestParam String viewerType // "CUSTOMER" hoặc "STORE"
    ) {
        chatService.deleteAllMessages(customerId, storeId, viewerType);
    }
    
    // ============= NEW: Customer-Admin Chat Endpoints =============
    
    // gửi tin nhắn trong 1 cuộc chat giữa customer và admin
    @PostMapping("/admin-conversations/{customerId}/{adminId}/messages")
    public ChatMessageResponse sendAdminMessage(
            @PathVariable UUID customerId,
            @PathVariable UUID adminId,
            @Valid @RequestBody ChatMessageRequest req
    ) {
        return chatService.sendAdminMessage(customerId, adminId, req);
    }

    // lấy lịch sử chat giữa customer và admin
    @GetMapping("/admin-conversations/{customerId}/{adminId}/messages")
    public List<ChatMessageResponse> getAdminMessages(
            @PathVariable UUID customerId,
            @PathVariable UUID adminId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam String viewerType
    ) {
        return chatService.getAdminMessages(customerId, adminId, limit, viewerType);
    }

    // ============= NEW: tất cả conversations của 1 customer với admin =============

    @GetMapping("/customers/{customerId}/admin-conversations")
    public List<ChatConversationResponse> getCustomerAdminConversations(
            @PathVariable UUID customerId
    ) {
        return chatService.getCustomerAdminConversations(customerId);
    }

    // ============= NEW: tất cả conversations của 1 admin =============

    @GetMapping("/admins/{adminId}/conversations")
    public List<ChatConversationResponse> getAdminConversations(
            @PathVariable UUID adminId
    ) {
        return chatService.getAdminConversations(adminId);
    }

    @PostMapping("/admin-conversations/{customerId}/{adminId}/read")
    public void markAdminRead(
            @PathVariable UUID customerId,
            @PathVariable UUID adminId,
            @RequestParam String viewerId // id của người đang xem tin nhắn
    ) {
        chatService.markAdminMessagesAsRead(customerId, adminId, viewerId);
    }

    @DeleteMapping("/admin-conversations/{customerId}/{adminId}/messages/{messageId}")
    public void deleteAdminMessage(
            @PathVariable UUID customerId,
            @PathVariable UUID adminId,
            @PathVariable String messageId,
            @RequestParam String viewerType // "CUSTOMER" hoặc "ADMIN"
    ) {
        chatService.deleteAdminMessage(customerId, adminId, messageId, viewerType);
    }

    @DeleteMapping("/admin-conversations/{customerId}/{adminId}/messages")
    public void deleteAllAdminMessages(
            @PathVariable UUID customerId,
            @PathVariable UUID adminId,
            @RequestParam String viewerType // "CUSTOMER" hoặc "ADMIN"
    ) {
        chatService.deleteAllAdminMessages(customerId, adminId, viewerType);
    }
    
    // ============= NEW: Store-Admin Chat Endpoints =============
    
    // gửi tin nhắn trong 1 cuộc chat giữa store và admin
    @PostMapping("/store-admin-conversations/{storeId}/{adminId}/messages")
    public ChatMessageResponse sendStoreAdminMessage(
            @PathVariable UUID storeId,
            @PathVariable UUID adminId,
            @Valid @RequestBody ChatMessageRequest req
    ) {
        return chatService.sendStoreAdminMessage(storeId, adminId, req);
    }

    // lấy lịch sử chat giữa store và admin
    @GetMapping("/store-admin-conversations/{storeId}/{adminId}/messages")
    public List<ChatMessageResponse> getStoreAdminMessages(
            @PathVariable UUID storeId,
            @PathVariable UUID adminId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam String viewerType
    ) {
        return chatService.getStoreAdminMessages(storeId, adminId, limit, viewerType);
    }

    // ============= NEW: tất cả conversations của 1 store với admin =============

    @GetMapping("/stores/{storeId}/admin-conversations")
    public List<ChatConversationResponse> getStoreAdminConversations(
            @PathVariable UUID storeId
    ) {
        return chatService.getStoreAdminConversations(storeId);
    }

    // ============= NEW: tất cả conversations của 1 admin với stores =============

    @GetMapping("/admins/{adminId}/store-conversations")
    public List<ChatConversationResponse> getAdminStoreConversations(
            @PathVariable UUID adminId
    ) {
        return chatService.getAdminStoreConversations(adminId);
    }

    @PostMapping("/store-admin-conversations/{storeId}/{adminId}/read")
    public void markStoreAdminRead(
            @PathVariable UUID storeId,
            @PathVariable UUID adminId,
            @RequestParam String viewerId // id của người đang xem tin nhắn
    ) {
        chatService.markStoreAdminMessagesAsRead(storeId, adminId, viewerId);
    }

    @DeleteMapping("/store-admin-conversations/{storeId}/{adminId}/messages/{messageId}")
    public void deleteStoreAdminMessage(
            @PathVariable UUID storeId,
            @PathVariable UUID adminId,
            @PathVariable String messageId,
            @RequestParam String viewerType // "STORE" hoặc "ADMIN"
    ) {
        chatService.deleteStoreAdminMessage(storeId, adminId, messageId, viewerType);
    }

    @DeleteMapping("/store-admin-conversations/{storeId}/{adminId}/messages")
    public void deleteAllStoreAdminMessages(
            @PathVariable UUID storeId,
            @PathVariable UUID adminId,
            @RequestParam String viewerType // "STORE" hoặc "ADMIN"
    ) {
        chatService.deleteAllStoreAdminMessages(storeId, adminId, viewerType);
    }

}