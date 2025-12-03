package org.example.audio_ecommerce.service.Impl;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Notification;
import org.example.audio_ecommerce.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebasePushServiceImpl implements FirebasePushService {

    @Value("${firebase.credentials}")
    private Resource firebaseConfigPath;

    @Value("${firebase.project-id}")
    private String projectId;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===========================================
    // 🔥 Lấy Access Token chuẩn FCM HTTP v1
    // ===========================================
    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(firebaseConfigPath.getInputStream())
                .createScoped(
                        "https://www.googleapis.com/auth/cloud-platform",
                        "https://www.googleapis.com/auth/firebase.messaging"
                );

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    // ===========================================
    // 🔥 Gửi noti tới 1 token
    // ===========================================
    @Override
    public void sendToToken(String token, Notification n, Map<String, String> extra) {
        if (token == null || token.isBlank()) return;

        try {
            String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

            Map<String, Object> body = Map.of(
                    "message", Map.of(
                            "token", token,
                            "notification", Map.of(
                                    "title", n.getTitle(),
                                    "body", n.getMessage()
                            ),
                            "data", extra != null ? extra : Map.of()
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(getAccessToken());

            HttpEntity<Object> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);

            log.info("FCM sent to {} => {}", token, resp.getStatusCode());
        } catch (Exception e) {
            log.error("FCM SEND FAILED: {}", e.getMessage());
        }
    }

    // ===========================================
    // 🔥 Gửi tới nhiều token
    // ===========================================
    @Override
    public void sendToTokens(Iterable<String> tokens, Notification n, Map<String, String> extra) {
        if (tokens == null) return;
        tokens.forEach(token -> sendToToken(token, n, extra));
    }
}
