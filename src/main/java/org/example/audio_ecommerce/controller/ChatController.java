// controller/ChatController.java
package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ChatMessageRequest;
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
}
