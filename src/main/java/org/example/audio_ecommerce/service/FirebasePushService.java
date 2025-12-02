package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.Notification;

import java.util.Map;

/**
 * Service gửi push notification qua Firebase Cloud Messaging (HTTP v1).
 */
public interface FirebasePushService {

    /**
     * Gửi notification tới 1 device token.
     */
    void sendToToken(String token,
                     Notification notification,
                     Map<String, String> extraData);

    /**
     * Gửi notification tới nhiều device token.
     */
    void sendToTokens(Iterable<String> tokens,
                      Notification notification,
                      Map<String, String> extraData);
}
