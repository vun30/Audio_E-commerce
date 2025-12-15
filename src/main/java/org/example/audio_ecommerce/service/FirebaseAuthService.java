package org.example.audio_ecommerce.service;

import com.google.firebase.auth.FirebaseToken;

public interface FirebaseAuthService {
    FirebaseToken verifyIdToken(String idToken);
}
