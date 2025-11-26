package org.example.audio_ecommerce.service.Impl;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.ChatMessageRequest;
import org.example.audio_ecommerce.dto.response.ChatMessageResponse;
import org.example.audio_ecommerce.service.ChatService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final Firestore firestore;

    private String buildConversationId(UUID customerId, UUID storeId) {
        return customerId + "_" + storeId;
    }

    @Override
    public ChatMessageResponse sendMessage(UUID customerId, UUID storeId, ChatMessageRequest req) {
        String conversationId = buildConversationId(customerId, storeId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", req.getSenderId());
        data.put("senderType", req.getSenderType());
        data.put("content", req.getContent());
        data.put("createdAt", Timestamp.now());
        data.put("read", false);

        try {
            // tạo message
            ApiFuture<DocumentReference> future = messagesRef.add(data);
            DocumentReference docRef = future.get();

            // update thông tin conversation (metadata)
            Map<String, Object> convMeta = new HashMap<>();
            convMeta.put("customerId", customerId.toString());
            convMeta.put("storeId", storeId.toString());
            convMeta.put("lastMessage", req.getContent());
            convMeta.put("lastMessageTime", Timestamp.now());
            firestore.collection("conversations")
                    .document(conversationId)
                    .set(convMeta, SetOptions.merge());

            return ChatMessageResponse.builder()
                    .id(docRef.getId())
                    .senderId(req.getSenderId())
                    .senderType(req.getSenderType())
                    .content(req.getContent())
                    .createdAt(Instant.now())
                    .read(false)
                    .build();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error sending chat message", e);
            throw new RuntimeException("Failed to send message");
        }
    }

    @Override
    public List<ChatMessageResponse> getMessages(UUID customerId, UUID storeId, int limit) {
        String conversationId = buildConversationId(customerId, storeId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        try {
            Query query = messagesRef
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit);

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            List<ChatMessageResponse> result = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                Timestamp ts = doc.getTimestamp("createdAt");
                result.add(ChatMessageResponse.builder()
                        .id(doc.getId())
                        .senderId(doc.getString("senderId"))
                        .senderType(doc.getString("senderType"))
                        .content(doc.getString("content"))
                        .createdAt(ts != null ? ts.toDate().toInstant() : null)
                        .read(Boolean.TRUE.equals(doc.getBoolean("read")))
                        .build());
            }

            // đảo lại cho đúng thứ tự cũ (từ cũ đến mới)
            Collections.reverse(result);
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error fetching chat messages", e);
            throw new RuntimeException("Failed to fetch messages");
        }
    }
}
