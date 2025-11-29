package org.example.audio_ecommerce.service.Impl;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.ChatMessageRequest;
import org.example.audio_ecommerce.dto.response.ChatConversationResponse;
import org.example.audio_ecommerce.dto.response.ChatMessageResponse;
import org.example.audio_ecommerce.entity.Enum.ChatMessageType;
import org.example.audio_ecommerce.service.ChatService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        data.put("type", req.getMessageType().name());

        // ========== MULTI MEDIA ==========
        if (req.getMessageType() == ChatMessageType.IMAGE || req.getMessageType() == ChatMessageType.MIXED) {
            if (req.getMediaUrl() != null) {
                List<Map<String, Object>> mediaData = new ArrayList<>();

                for (ChatMessageRequest.MediaItem item : req.getMediaUrl()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("url", item.getUrl());
                    m.put("type", item.getType());
                    mediaData.add(m);
                }
                data.put("mediaUrl", mediaData);
            }
        }

        // ========== PRODUCT ==========
        if (req.getMessageType() == ChatMessageType.PRODUCT) {
            if (req.getProductId() != null)
                data.put("productId", req.getProductId().toString());

            data.put("productName", req.getProductName());
            data.put("productImage", req.getProductImage());
            data.put("productPrice",
                    req.getProductPrice() != null ? req.getProductPrice().toPlainString() : null);
        }

        try {
            DocumentReference docRef = messagesRef.add(data).get();

            // UPDATE conversation metadata
            Map<String, Object> convMeta = new HashMap<>();
            convMeta.put("customerId", customerId.toString());
            convMeta.put("storeId", storeId.toString());
            convMeta.put("lastMessage", req.getContent());
            convMeta.put("lastMessageTime", Timestamp.now());
            firestore.collection("conversations")
                    .document(conversationId)
                    .set(convMeta, SetOptions.merge());

            // Build mediaList response
            List<ChatMessageResponse.MediaItem> mediaList = null;
            if (req.getMediaUrl() != null) {
                mediaList = req.getMediaUrl().stream().map(m ->
                        ChatMessageResponse.MediaItem.builder()
                                .url(m.getUrl())
                                .type(m.getType())
                                .build()
                ).toList();
            }

            return ChatMessageResponse.builder()
                    .id(docRef.getId())
                    .senderId(req.getSenderId())
                    .senderType(req.getSenderType())
                    .content(req.getContent())
                    .createdAt(Instant.now())
                    .read(false)
                    .messageType(req.getMessageType())
                    .mediaUrl(mediaList)
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .productImage(req.getProductImage())
                    .productPrice(req.getProductPrice())
                    .build();

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send message", e);
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
            List<QueryDocumentSnapshot> docs = messagesRef
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            List<ChatMessageResponse> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {

                // Parse media list
                List<Map<String, Object>> mediaData =
                        (List<Map<String, Object>>) doc.get("mediaUrl");

                List<ChatMessageResponse.MediaItem> mediaList = new ArrayList<>();
                if (mediaData != null) {
                    for (Map<String, Object> m : mediaData) {
                        mediaList.add(ChatMessageResponse.MediaItem.builder()
                                .url((String) m.get("url"))
                                .type((String) m.get("type"))
                                .build());
                    }
                }

                // Parse product price
                BigDecimal price = null;
                String priceStr = doc.getString("productPrice");
                if (priceStr != null) {
                    price = new BigDecimal(priceStr);
                }

                result.add(ChatMessageResponse.builder()
                        .id(doc.getId())
                        .senderId(doc.getString("senderId"))
                        .senderType(doc.getString("senderType"))
                        .content(doc.getString("content"))
                        .createdAt(doc.getTimestamp("createdAt").toDate().toInstant())
                        .read(Boolean.TRUE.equals(doc.getBoolean("read")))
                        .messageType(ChatMessageType.valueOf(doc.getString("type")))
                        .mediaUrl(mediaList)
                        .productId(doc.getString("productId") != null ?
                                UUID.fromString(doc.getString("productId")) : null)
                        .productName(doc.getString("productName"))
                        .productImage(doc.getString("productImage"))
                        .productPrice(price)
                        .build());
            }

            Collections.reverse(result);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
    }

    @Override
    public void markMessagesAsRead(UUID customerId, UUID storeId, String viewerId) {

        String conversationId = buildConversationId(customerId, storeId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        try {
            // Lấy tất cả tin chưa đọc
            List<QueryDocumentSnapshot> docs = messagesRef
                    .whereEqualTo("read", false)
                    .get()
                    .get()
                    .getDocuments();

            WriteBatch batch = firestore.batch();

            for (QueryDocumentSnapshot doc : docs) {

                // Chỉ mark READ những tin của đối phương
                String senderId = doc.getString("senderId");

                if (senderId != null && !senderId.equals(viewerId)) {
                    batch.update(doc.getReference(), "read", true);
                }
            }

            batch.commit();

        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            throw new RuntimeException("Failed to mark messages as read");
        }
    }


    @Override
    public List<ChatConversationResponse> getCustomerConversations(UUID customerId) {
        return getConversationList("customerId", customerId.toString());
    }

    @Override
    public List<ChatConversationResponse> getStoreConversations(UUID storeId) {
        return getConversationList("storeId", storeId.toString());
    }

    private List<ChatConversationResponse> getConversationList(String field, String id) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection("conversations")
                    .whereEqualTo(field, id)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments();

            List<ChatConversationResponse> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                result.add(ChatConversationResponse.builder()
                        .id(doc.getId())
                        .customerId(UUID.fromString(doc.getString("customerId")))
                        .storeId(UUID.fromString(doc.getString("storeId")))
                        .lastMessage(doc.getString("lastMessage"))
                        .lastMessageTime(doc.getTimestamp("lastMessageTime").toDate().toInstant())
                        .build());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch conversation list");
        }
    }
}
