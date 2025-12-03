package org.example.audio_ecommerce.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;

@Component
public class FirebaseFileLoader {

    @Value("${FIREBASE_CREDENTIALS:}")
    private String firebaseJson;

    @PostConstruct
    public void createFirebaseJson() throws Exception {

        if (firebaseJson == null || firebaseJson.isBlank()) {
            System.out.println("⚠ FIREBASE_CREDENTIALS is missing!");
            return;
        }

        String path = "/var/run/secrets/firebase.json";

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(firebaseJson);   // ghi nguyên JSON vào file
        }

        System.out.println("✅ Firebase JSON file created at: " + path);
    }
}
