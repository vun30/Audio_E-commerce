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
import org.example.audio_ecommerce.util.MessageFilterUtil;
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
    
    // New method to build conversation ID for customer-admin chat
    private String buildAdminConversationId(UUID customerId, UUID adminId) {
        return "admin_" + customerId + "_" + adminId;
    }
    
    // New method to build conversation ID for store-admin chat
    private String buildStoreAdminConversationId(UUID storeId, UUID adminId) {
        return "store_admin_" + storeId + "_" + adminId;
    }

    @Override
    public ChatMessageResponse sendMessage(UUID customerId, UUID storeId, ChatMessageRequest req) {
        // Validate message content - throw exception if sensitive content is detected
        MessageFilterUtil.validateMessageContent(req.getContent());
        
        // If validation passes, filter the content (as a safety measure)
        String filteredContent = MessageFilterUtil.filterSensitiveContent(req.getContent());
        req.setContent(filteredContent);

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
        data.put("deletedForCustomer", false);
        data.put("deletedForStore", false);

        // ========== MULTI MEDIA ==========
        if (req.getMessageType() == ChatMessageType.IMAGE
                || req.getMessageType() == ChatMessageType.VIDEO
                || req.getMessageType() == ChatMessageType.MIXED) {
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

            String senderType = req.getSenderType(); // ví dụ: "CUSTOMER" / "STORE"

            if ("CUSTOMER".equalsIgnoreCase(senderType)) {
                // customer gửi -> store là người chưa đọc
                convMeta.put("storeUnreadCount", FieldValue.increment(1L));
            } else if ("STORE".equalsIgnoreCase(senderType)) {
                // store gửi -> customer là người chưa đọc
                convMeta.put("customerUnreadCount", FieldValue.increment(1L));
            }

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
    public ChatMessageResponse sendAdminMessage(UUID customerId, UUID adminId, ChatMessageRequest req) {
        // Validate message content - throw exception if sensitive content is detected
        MessageFilterUtil.validateMessageContent(req.getContent());
        
        // If validation passes, filter the content (as a safety measure)
        String filteredContent = MessageFilterUtil.filterSensitiveContent(req.getContent());
        req.setContent(filteredContent);

        String conversationId = buildAdminConversationId(customerId, adminId);

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
        data.put("deletedForCustomer", false);
        data.put("deletedForAdmin", false);

        // ========== MULTI MEDIA ==========
        if (req.getMessageType() == ChatMessageType.IMAGE
                || req.getMessageType() == ChatMessageType.VIDEO
                || req.getMessageType() == ChatMessageType.MIXED) {
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
            convMeta.put("adminId", adminId.toString());
            convMeta.put("lastMessage", req.getContent());
            convMeta.put("lastMessageTime", Timestamp.now());

            String senderType = req.getSenderType();

            if ("CUSTOMER".equalsIgnoreCase(senderType)) {
                // customer gửi -> admin là người chưa đọc
                convMeta.put("adminUnreadCount", FieldValue.increment(1L));
            } else if ("ADMIN".equalsIgnoreCase(senderType)) {
                // admin gửi -> customer là người chưa đọc
                convMeta.put("customerUnreadCount", FieldValue.increment(1L));
            }

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
            throw new RuntimeException("Failed to send admin message", e);
        }
    }
    
    @Override
    public ChatMessageResponse sendStoreAdminMessage(UUID storeId, UUID adminId, ChatMessageRequest req) {
        // Validate message content - throw exception if sensitive content is detected
        MessageFilterUtil.validateMessageContent(req.getContent());
        
        // If validation passes, filter the content (as a safety measure)
        String filteredContent = MessageFilterUtil.filterSensitiveContent(req.getContent());
        req.setContent(filteredContent);

        String conversationId = buildStoreAdminConversationId(storeId, adminId);

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
        data.put("deletedForStore", false);
        data.put("deletedForAdmin", false);

        // ========== MULTI MEDIA ==========
        if (req.getMessageType() == ChatMessageType.IMAGE
                || req.getMessageType() == ChatMessageType.VIDEO
                || req.getMessageType() == ChatMessageType.MIXED) {
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
            convMeta.put("storeId", storeId.toString());
            convMeta.put("adminId", adminId.toString());
            convMeta.put("lastMessage", req.getContent());
            convMeta.put("lastMessageTime", Timestamp.now());

            String senderType = req.getSenderType();

            if ("STORE".equalsIgnoreCase(senderType)) {
                // store gửi -> admin là người chưa đọc
                convMeta.put("adminUnreadCount", FieldValue.increment(1L));
            } else if ("ADMIN".equalsIgnoreCase(senderType)) {
                // admin gửi -> store là người chưa đọc
                convMeta.put("storeUnreadCount", FieldValue.increment(1L));
            }

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
            throw new RuntimeException("Failed to send store-admin message", e);
        }
    }

    @Override
    public List<ChatMessageResponse> getMessages(UUID customerId, UUID storeId, int limit, String viewerType) {

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
            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(viewerType);
            boolean isStore = "STORE".equalsIgnoreCase(viewerType);
            for (QueryDocumentSnapshot doc : docs) {

                Boolean deletedForCustomer = doc.getBoolean("deletedForCustomer");
                Boolean deletedForStore = doc.getBoolean("deletedForStore");

                if (isCustomer && Boolean.TRUE.equals(deletedForCustomer)) {
                    continue;
                }
                if (isStore && Boolean.TRUE.equals(deletedForStore)) {
                    continue;
                }

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
                        .deletedForCustomer(Boolean.TRUE.equals(deletedForCustomer))
                        .deletedForStore(Boolean.TRUE.equals(deletedForStore))
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
    public List<ChatMessageResponse> getAdminMessages(UUID customerId, UUID adminId, int limit, String viewerType) {
        String conversationId = buildAdminConversationId(customerId, adminId);

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
            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(viewerType);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(viewerType);
            for (QueryDocumentSnapshot doc : docs) {

                Boolean deletedForCustomer = doc.getBoolean("deletedForCustomer");
                Boolean deletedForAdmin = doc.getBoolean("deletedForAdmin");

                if (isCustomer && Boolean.TRUE.equals(deletedForCustomer)) {
                    continue;
                }
                if (isAdmin && Boolean.TRUE.equals(deletedForAdmin)) {
                    continue;
                }

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
                        .deletedForCustomer(Boolean.TRUE.equals(deletedForCustomer))
                        .deletedForAdmin(Boolean.TRUE.equals(deletedForAdmin))
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
            throw new RuntimeException("Failed to fetch admin messages", e);
        }
    }
    
    @Override
    public List<ChatMessageResponse> getStoreAdminMessages(UUID storeId, UUID adminId, int limit, String viewerType) {
        String conversationId = buildStoreAdminConversationId(storeId, adminId);

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
            boolean isStore = "STORE".equalsIgnoreCase(viewerType);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(viewerType);
            for (QueryDocumentSnapshot doc : docs) {

                Boolean deletedForStore = doc.getBoolean("deletedForStore");
                Boolean deletedForAdmin = doc.getBoolean("deletedForAdmin");

                if (isStore && Boolean.TRUE.equals(deletedForStore)) {
                    continue;
                }
                if (isAdmin && Boolean.TRUE.equals(deletedForAdmin)) {
                    continue;
                }

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
                        .deletedForStore(Boolean.TRUE.equals(deletedForStore))
                        .deletedForAdmin(Boolean.TRUE.equals(deletedForAdmin))
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
            throw new RuntimeException("Failed to fetch store-admin messages", e);
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
            int updatedCount = 0;
            for (QueryDocumentSnapshot doc : docs) {
                // Chỉ mark READ những tin của đối phương
                String senderId = doc.getString("senderId");

                if (senderId != null && !senderId.equals(viewerId)) {
                    batch.update(doc.getReference(), "read", true);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                batch.commit().get(); // đợi commit xong cho chắc
            }

            if (updatedCount > 0) {
                boolean viewerIsCustomer = viewerId.equals(customerId.toString());
                String fieldToDecrease = viewerIsCustomer
                        ? "customerUnreadCount"  // customer đang xem -> giảm số tin chưa đọc của customer
                        : "storeUnreadCount";    // store đang xem -> giảm số tin chưa đọc của store

                Map<String, Object> convMeta = new HashMap<>();
                convMeta.put(fieldToDecrease, FieldValue.increment(- (long) updatedCount));

                firestore.collection("conversations")
                        .document(conversationId)
                        .set(convMeta, SetOptions.merge());
            }

        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            throw new RuntimeException("Failed to mark messages as read");
        }
    }
    
    @Override
    public void markAdminMessagesAsRead(UUID customerId, UUID adminId, String viewerId) {
        String conversationId = buildAdminConversationId(customerId, adminId);

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
            int updatedCount = 0;
            for (QueryDocumentSnapshot doc : docs) {
                // Chỉ mark READ những tin của đối phương
                String senderId = doc.getString("senderId");

                if (senderId != null && !senderId.equals(viewerId)) {
                    batch.update(doc.getReference(), "read", true);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                batch.commit().get(); // đợi commit xong cho chắc
            }

            if (updatedCount > 0) {
                boolean viewerIsCustomer = viewerId.equals(customerId.toString());
                String fieldToDecrease = viewerIsCustomer
                        ? "customerUnreadCount"  // customer đang xem -> giảm số tin chưa đọc của customer
                        : "adminUnreadCount";    // admin đang xem -> giảm số tin chưa đọc của admin

                Map<String, Object> convMeta = new HashMap<>();
                convMeta.put(fieldToDecrease, FieldValue.increment(- (long) updatedCount));

                firestore.collection("conversations")
                        .document(conversationId)
                        .set(convMeta, SetOptions.merge());
            }

        } catch (Exception e) {
            log.error("Error marking admin messages as read", e);
            throw new RuntimeException("Failed to mark admin messages as read");
        }
    }
    
    @Override
    public void markStoreAdminMessagesAsRead(UUID storeId, UUID adminId, String viewerId) {
        String conversationId = buildStoreAdminConversationId(storeId, adminId);

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
            int updatedCount = 0;
            for (QueryDocumentSnapshot doc : docs) {
                // Chỉ mark READ những tin của đối phương
                String senderId = doc.getString("senderId");

                if (senderId != null && !senderId.equals(viewerId)) {
                    batch.update(doc.getReference(), "read", true);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                batch.commit().get(); // đợi commit xong cho chắc
            }

            if (updatedCount > 0) {
                boolean viewerIsStore = viewerId.equals(storeId.toString());
                String fieldToDecrease = viewerIsStore
                        ? "storeUnreadCount"  // store đang xem -> giảm số tin chưa đọc của store
                        : "adminUnreadCount";    // admin đang xem -> giảm số tin chưa đọc của admin

                Map<String, Object> convMeta = new HashMap<>();
                convMeta.put(fieldToDecrease, FieldValue.increment(- (long) updatedCount));

                firestore.collection("conversations")
                        .document(conversationId)
                        .set(convMeta, SetOptions.merge());
            }

        } catch (Exception e) {
            log.error("Error marking store-admin messages as read", e);
            throw new RuntimeException("Failed to mark store-admin messages as read");
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
    
    @Override
    public List<ChatConversationResponse> getCustomerAdminConversations(UUID customerId) {
        return getAdminConversationList("customerId", customerId.toString());
    }
    
    @Override
    public List<ChatConversationResponse> getAdminConversations(UUID adminId) {
        return getAdminConversationList("adminId", adminId.toString());
    }
    
    @Override
    public List<ChatConversationResponse> getStoreAdminConversations(UUID storeId) {
        return getStoreAdminConversationList("storeId", storeId.toString());
    }
    
    @Override
    public List<ChatConversationResponse> getAdminStoreConversations(UUID adminId) {
        return getStoreAdminConversationList("adminId", adminId.toString());
    }

    @Override
    public void deleteMessage(UUID customerId, UUID storeId, String messageId, String viewerType) {

        String conversationId = buildConversationId(customerId, storeId);

        DocumentReference msgRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);

        try {
            DocumentSnapshot snap = msgRef.get().get();
            if (!snap.exists()) {
                throw new RuntimeException("Message not found");
            }

            Boolean deletedForCustomer = snap.getBoolean("deletedForCustomer");
            Boolean deletedForStore = snap.getBoolean("deletedForStore");

            Map<String, Object> update = new HashMap<>();
            if ("CUSTOMER".equalsIgnoreCase(viewerType)) {
                update.put("deletedForCustomer", true);
            } else if ("STORE".equalsIgnoreCase(viewerType)) {
                update.put("deletedForStore", true);
            } else {
                throw new IllegalArgumentException("viewerType must be CUSTOMER or STORE");
            }

            msgRef.set(update, SetOptions.merge()).get();

            // OPTIONAL: nếu cả 2 bên đều đã xoá -> xoá hẳn document cho nhẹ DB
            deletedForCustomer = "CUSTOMER".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForCustomer);
            deletedForStore = "STORE".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForStore);

            if (deletedForCustomer && deletedForStore) {
                msgRef.delete(); // không cần .get() cũng được
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete message (soft)", e);
        }
    }
    
    @Override
    public void deleteAdminMessage(UUID customerId, UUID adminId, String messageId, String viewerType) {
        String conversationId = buildAdminConversationId(customerId, adminId);

        DocumentReference msgRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);

        try {
            DocumentSnapshot snap = msgRef.get().get();
            if (!snap.exists()) {
                throw new RuntimeException("Message not found");
            }

            Boolean deletedForCustomer = snap.getBoolean("deletedForCustomer");
            Boolean deletedForAdmin = snap.getBoolean("deletedForAdmin");

            Map<String, Object> update = new HashMap<>();
            if ("CUSTOMER".equalsIgnoreCase(viewerType)) {
                update.put("deletedForCustomer", true);
            } else if ("ADMIN".equalsIgnoreCase(viewerType)) {
                update.put("deletedForAdmin", true);
            } else {
                throw new IllegalArgumentException("viewerType must be CUSTOMER or ADMIN");
            }

            msgRef.set(update, SetOptions.merge()).get();

            // OPTIONAL: nếu cả 2 bên đều đã xoá -> xoá hẳn document cho nhẹ DB
            deletedForCustomer = "CUSTOMER".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForCustomer);
            deletedForAdmin = "ADMIN".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForAdmin);

            if (deletedForCustomer && deletedForAdmin) {
                msgRef.delete(); // không cần .get() cũng được
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete admin message (soft)", e);
        }
    }
    
    @Override
    public void deleteStoreAdminMessage(UUID storeId, UUID adminId, String messageId, String viewerType) {
        String conversationId = buildStoreAdminConversationId(storeId, adminId);

        DocumentReference msgRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);

        try {
            DocumentSnapshot snap = msgRef.get().get();
            if (!snap.exists()) {
                throw new RuntimeException("Message not found");
            }

            Boolean deletedForStore = snap.getBoolean("deletedForStore");
            Boolean deletedForAdmin = snap.getBoolean("deletedForAdmin");

            Map<String, Object> update = new HashMap<>();
            if ("STORE".equalsIgnoreCase(viewerType)) {
                update.put("deletedForStore", true);
            } else if ("ADMIN".equalsIgnoreCase(viewerType)) {
                update.put("deletedForAdmin", true);
            } else {
                throw new IllegalArgumentException("viewerType must be STORE or ADMIN");
            }

            msgRef.set(update, SetOptions.merge()).get();

            // OPTIONAL: nếu cả 2 bên đều đã xoá -> xoá hẳn document cho nhẹ DB
            deletedForStore = "STORE".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForStore);
            deletedForAdmin = "ADMIN".equalsIgnoreCase(viewerType) ? true : Boolean.TRUE.equals(deletedForAdmin);

            if (deletedForStore && deletedForAdmin) {
                msgRef.delete(); // không cần .get() cũng được
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete store-admin message (soft)", e);
        }
    }

    @Override
    public void deleteAllMessages(UUID customerId, UUID storeId, String viewerType) {

        String conversationId = buildConversationId(customerId, storeId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        try {
            List<QueryDocumentSnapshot> docs = messagesRef
                    .get()
                    .get()
                    .getDocuments();

            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(viewerType);
            boolean isStore = "STORE".equalsIgnoreCase(viewerType);

            if (!isCustomer && !isStore) {
                throw new IllegalArgumentException("viewerType must be CUSTOMER or STORE");
            }

            WriteBatch batch = firestore.batch();

            // =========================
            // 1) XÓA SOFT HOẶC HARD PER MESSAGE
            // =========================
            for (QueryDocumentSnapshot doc : docs) {
                Boolean deletedForCustomer = doc.getBoolean("deletedForCustomer");
                Boolean deletedForStore = doc.getBoolean("deletedForStore");

                boolean afterCus = deletedForCustomer != null && deletedForCustomer;
                boolean afterStore = deletedForStore != null && deletedForStore;

                // soft delete
                Map<String, Object> update = new HashMap<>();
                if (isCustomer) {
                    update.put("deletedForCustomer", true);
                    afterCus = true;
                }
                if (isStore) {
                    update.put("deletedForStore", true);
                    afterStore = true;
                }

                batch.set(doc.getReference(), update, SetOptions.merge());

                // nếu cả 2 bên xoá thì hard delete
                if (afterCus && afterStore) {
                    batch.delete(doc.getReference());
                }
            }

            batch.commit().get();

            // =========================
            // 2) RESET LAST MESSAGE + UNREAD COUNT
            // =========================
            Map<String, Object> resetMeta = new HashMap<>();
            resetMeta.put("lastMessage", "");
            resetMeta.put("lastMessageTime", null);
            resetMeta.put("customerUnreadCount", 0L);
            resetMeta.put("storeUnreadCount", 0L);

            firestore.collection("conversations")
                    .document(conversationId)
                    .set(resetMeta, SetOptions.merge());

        } catch (Exception e) {
            log.error("Error deleting all messages", e);
            throw new RuntimeException("Failed to delete all messages");
        }
    }
    
    @Override
    public void deleteAllAdminMessages(UUID customerId, UUID adminId, String viewerType) {
        String conversationId = buildAdminConversationId(customerId, adminId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        try {
            List<QueryDocumentSnapshot> docs = messagesRef
                    .get()
                    .get()
                    .getDocuments();

            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(viewerType);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(viewerType);

            if (!isCustomer && !isAdmin) {
                throw new IllegalArgumentException("viewerType must be CUSTOMER or ADMIN");
            }

            WriteBatch batch = firestore.batch();

            // =========================
            // 1) XÓA SOFT HOẶC HARD PER MESSAGE
            // =========================
            for (QueryDocumentSnapshot doc : docs) {
                Boolean deletedForCustomer = doc.getBoolean("deletedForCustomer");
                Boolean deletedForAdmin = doc.getBoolean("deletedForAdmin");

                boolean afterCus = deletedForCustomer != null && deletedForCustomer;
                boolean afterAdmin = deletedForAdmin != null && deletedForAdmin;

                // soft delete
                Map<String, Object> update = new HashMap<>();
                if (isCustomer) {
                    update.put("deletedForCustomer", true);
                    afterCus = true;
                }
                if (isAdmin) {
                    update.put("deletedForAdmin", true);
                    afterAdmin = true;
                }

                batch.set(doc.getReference(), update, SetOptions.merge());

                // nếu cả 2 bên xoá thì hard delete
                if (afterCus && afterAdmin) {
                    batch.delete(doc.getReference());
                }
            }

            batch.commit().get();

            // =========================
            // 2) RESET LAST MESSAGE + UNREAD COUNT
            // =========================
            Map<String, Object> resetMeta = new HashMap<>();
            resetMeta.put("lastMessage", "");
            resetMeta.put("lastMessageTime", null);
            resetMeta.put("customerUnreadCount", 0L);
            resetMeta.put("adminUnreadCount", 0L);

            firestore.collection("conversations")
                    .document(conversationId)
                    .set(resetMeta, SetOptions.merge());

        } catch (Exception e) {
            log.error("Error deleting all admin messages", e);
            throw new RuntimeException("Failed to delete all admin messages");
        }
    }
    
    @Override
    public void deleteAllStoreAdminMessages(UUID storeId, UUID adminId, String viewerType) {
        String conversationId = buildStoreAdminConversationId(storeId, adminId);

        CollectionReference messagesRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages");

        try {
            List<QueryDocumentSnapshot> docs = messagesRef
                    .get()
                    .get()
                    .getDocuments();

            boolean isStore = "STORE".equalsIgnoreCase(viewerType);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(viewerType);

            if (!isStore && !isAdmin) {
                throw new IllegalArgumentException("viewerType must be STORE or ADMIN");
            }

            WriteBatch batch = firestore.batch();

            // =========================
            // 1) XÓA SOFT HOẶC HARD PER MESSAGE
            // =========================
            for (QueryDocumentSnapshot doc : docs) {
                Boolean deletedForStore = doc.getBoolean("deletedForStore");
                Boolean deletedForAdmin = doc.getBoolean("deletedForAdmin");

                boolean afterStore = deletedForStore != null && deletedForStore;
                boolean afterAdmin = deletedForAdmin != null && deletedForAdmin;

                // soft delete
                Map<String, Object> update = new HashMap<>();
                if (isStore) {
                    update.put("deletedForStore", true);
                    afterStore = true;
                }
                if (isAdmin) {
                    update.put("deletedForAdmin", true);
                    afterAdmin = true;
                }

                batch.set(doc.getReference(), update, SetOptions.merge());

                // nếu cả 2 bên xoá thì hard delete
                if (afterStore && afterAdmin) {
                    batch.delete(doc.getReference());
                }
            }

            batch.commit().get();

            // =========================
            // 2) RESET LAST MESSAGE + UNREAD COUNT
            // =========================
            Map<String, Object> resetMeta = new HashMap<>();
            resetMeta.put("lastMessage", "");
            resetMeta.put("lastMessageTime", null);
            resetMeta.put("storeUnreadCount", 0L);
            resetMeta.put("adminUnreadCount", 0L);

            firestore.collection("conversations")
                    .document(conversationId)
                    .set(resetMeta, SetOptions.merge());

        } catch (Exception e) {
            log.error("Error deleting all store-admin messages", e);
            throw new RuntimeException("Failed to delete all store-admin messages");
        }
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
                // Skip admin conversations
                if (doc.getId().startsWith("admin_")) {
                    continue;
                }
                
                Long cusUnread = doc.getLong("customerUnreadCount");
                Long storeUnread = doc.getLong("storeUnreadCount");
                result.add(ChatConversationResponse.builder()
                        .id(doc.getId())
                        .customerId(UUID.fromString(doc.getString("customerId")))
                        .storeId(UUID.fromString(doc.getString("storeId")))
                        .lastMessage(doc.getString("lastMessage"))
                        .lastMessageTime(doc.getTimestamp("lastMessageTime").toDate().toInstant())
                        .customerUnreadCount(cusUnread != null ? cusUnread : 0L)
                        .storeUnreadCount(storeUnread != null ? storeUnread : 0L)
                        .build());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch conversation list");
        }
    }
    
    private List<ChatConversationResponse> getAdminConversationList(String field, String id) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection("conversations")
                    .whereEqualTo(field, id)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments();

            List<ChatConversationResponse> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                // Only include admin conversations (customer-admin)
                if (!doc.getId().startsWith("admin_") || doc.getId().startsWith("store_admin_")) {
                    continue;
                }
                
                Long cusUnread = doc.getLong("customerUnreadCount");
                Long adminUnread = doc.getLong("adminUnreadCount");
                result.add(ChatConversationResponse.builder()
                        .id(doc.getId())
                        .customerId(UUID.fromString(doc.getString("customerId")))
                        .adminId(UUID.fromString(doc.getString("adminId")))
                        .lastMessage(doc.getString("lastMessage"))
                        .lastMessageTime(doc.getTimestamp("lastMessageTime").toDate().toInstant())
                        .customerUnreadCount(cusUnread != null ? cusUnread : 0L)
                        .adminUnreadCount(adminUnread != null ? adminUnread : 0L)
                        .build());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch admin conversation list");
        }
    }
    
    private List<ChatConversationResponse> getStoreAdminConversationList(String field, String id) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection("conversations")
                    .whereEqualTo(field, id)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments();

            List<ChatConversationResponse> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                // Only include store-admin conversations
                if (!doc.getId().startsWith("store_admin_")) {
                    continue;
                }
                
                Long storeUnread = doc.getLong("storeUnreadCount");
                Long adminUnread = doc.getLong("adminUnreadCount");
                result.add(ChatConversationResponse.builder()
                        .id(doc.getId())
                        .storeId(UUID.fromString(doc.getString("storeId")))
                        .adminId(UUID.fromString(doc.getString("adminId")))
                        .lastMessage(doc.getString("lastMessage"))
                        .lastMessageTime(doc.getTimestamp("lastMessageTime").toDate().toInstant())
                        .storeUnreadCount(storeUnread != null ? storeUnread : 0L)
                        .adminUnreadCount(adminUnread != null ? adminUnread : 0L)
                        .build());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch store-admin conversation list");
        }
    }
}