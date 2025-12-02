// service/NotificationCreatorService.java
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.entity.Enum.NotificationType;
import org.example.audio_ecommerce.entity.Notification;

import java.util.Map;
import java.util.UUID;

public interface NotificationCreatorService {

    Notification createAndSend(
            NotificationTarget target,
            UUID targetId,
            NotificationType type,
            String title,
            String message,
            String actionUrl,
            String metadataJson,
            Map<String, String> extraData   // cho FCM data nếu cần
    );
}
