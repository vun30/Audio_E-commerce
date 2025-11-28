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
            @RequestParam(defaultValue = "50") int limit
    ) {
        return chatService.getMessages(customerId, storeId, limit);
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

}
