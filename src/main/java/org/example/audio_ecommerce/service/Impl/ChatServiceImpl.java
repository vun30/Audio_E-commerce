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

        if (req.getMessageType() == ChatMessageType.PRODUCT) {
            if (req.getProductId() != null) {
                data.put("productId", req.getProductId().toString());
            }
            data.put("productName", req.getProductName());
            data.put("productImage", req.getProductImage());
            data.put("productPrice", req.getProductPrice() != null ? req.getProductPrice().toPlainString() : null);
        }

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
                    .messageType(req.getMessageType())
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .productImage(req.getProductImage())
                    .productPrice(req.getProductPrice())
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

                String typeStr = doc.getString("type");
                ChatMessageType type = null;
                if (typeStr != null) {
                    try {
                        type = ChatMessageType.valueOf(typeStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                String productIdStr = doc.getString("productId");

                String productPriceStr = doc.getString("productPrice");
                BigDecimal productPrice = null;
                if (productPriceStr != null) {
                    productPrice = new BigDecimal(productPriceStr);
                }

                result.add(ChatMessageResponse.builder()
                        .id(doc.getId())
                        .senderId(doc.getString("senderId"))
                        .senderType(doc.getString("senderType"))
                        .content(doc.getString("content"))
                        .createdAt(ts != null ? ts.toDate().toInstant() : null)
                        .read(Boolean.TRUE.equals(doc.getBoolean("read")))
                        .messageType(type)
                        .productId(productIdStr != null ? UUID.fromString(productIdStr) : null)
                        .productName(doc.getString("productName"))
                        .productImage(doc.getString("productImage"))
                        .productPrice(productPrice)
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
    @Override
    public List<ChatConversationResponse> getCustomerConversations(UUID customerId) {
        CollectionReference convRef = firestore.collection("conversations");
        try {
            Query query = convRef
                    .whereEqualTo("customerId", customerId.toString())
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING);

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            List<ChatConversationResponse> result = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                result.add(toConversationResponse(doc));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error fetching conversations by customer", e);
            throw new RuntimeException("Failed to fetch conversations");
        }
    }

    // =============== NEW: list conversation theo store ===============

    @Override
    public List<ChatConversationResponse> getStoreConversations(UUID storeId) {
        CollectionReference convRef = firestore.collection("conversations");
        try {
            Query query = convRef
                    .whereEqualTo("storeId", storeId.toString())
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING);

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            List<ChatConversationResponse> result = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                result.add(toConversationResponse(doc));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error fetching conversations by store", e);
            throw new RuntimeException("Failed to fetch conversations");
        }
    }

    // =============== Helper mapper ===============

    private ChatConversationResponse toConversationResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String customerIdStr = doc.getString("customerId");
        String storeIdStr = doc.getString("storeId");
        String lastMessage = doc.getString("lastMessage");
        Timestamp ts = doc.getTimestamp("lastMessageTime");

        return ChatConversationResponse.builder()
                .id(id)
                .customerId(customerIdStr != null ? UUID.fromString(customerIdStr) : null)
                .storeId(storeIdStr != null ? UUID.fromString(storeIdStr) : null)
                .lastMessage(lastMessage)
                .lastMessageTime(ts != null ? ts.toDate().toInstant() : null)
                .build();
    }
}
