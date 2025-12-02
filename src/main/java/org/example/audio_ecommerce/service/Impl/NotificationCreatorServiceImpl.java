// service/Impl/NotificationCreatorServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.DeviceToken;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.entity.Enum.NotificationType;
import org.example.audio_ecommerce.entity.Notification;
import org.example.audio_ecommerce.repository.DeviceTokenRepository;
import org.example.audio_ecommerce.repository.NotificationRepository;
import org.example.audio_ecommerce.service.FirebasePushService;
import org.example.audio_ecommerce.service.NotificationCreatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreatorServiceImpl implements NotificationCreatorService {

    private final NotificationRepository notificationRepo;
    private final DeviceTokenRepository deviceTokenRepo;
    private final FirebasePushService firebasePushService;

    @Override
    @Transactional
    public Notification createAndSend(NotificationTarget target,
                                      UUID targetId,
                                      NotificationType type,
                                      String title,
                                      String message,
                                      String actionUrl,
                                      String metadataJson,
                                      Map<String, String> extraData) {

        Notification notif = Notification.builder()
                .target(target)
                .targetId(targetId)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .metadataJson(metadataJson)
                .read(false)
                .build();

        Notification saved = notificationRepo.save(notif);

        List<DeviceToken> tokens =
                deviceTokenRepo.findByTargetAndTargetId(target, targetId);

        List<String> tokenStrings = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        firebasePushService.sendToTokens(tokenStrings, saved, extraData);

        return saved;
    }
}
