package org.example.audio_ecommerce.service.Impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.service.FirebaseAuthService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    @Override
    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.error("Firebase token invalid", e);
            throw new IllegalArgumentException("Firebase ID token không hợp lệ");
        }
    }
}
