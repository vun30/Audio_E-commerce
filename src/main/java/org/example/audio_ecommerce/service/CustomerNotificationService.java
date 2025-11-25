package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CustomerNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerNotificationService {

    Page<NotificationResponse> listForCurrentCustomer(String keyword, Boolean read, Pageable pageable);

    long countUnreadForCurrentCustomer();

    void markAsRead(UUID notificationId);

    void markAllAsReadForCurrentCustomer();

    void delete(UUID notificationId);

    void performAction(UUID notificationId, CustomerNotificationActionRequest req);
}
