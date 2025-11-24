package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.StoreNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StoreNotificationService {

    Page<NotificationResponse> listForCurrentStore(String keyword, Boolean read, Pageable pageable);

    long countUnreadForCurrentStore();

    void markAsRead(UUID notificationId);

    void markAllAsReadForCurrentStore();

    void delete(UUID notificationId);

    void performAction(UUID notificationId, StoreNotificationActionRequest req);
}
