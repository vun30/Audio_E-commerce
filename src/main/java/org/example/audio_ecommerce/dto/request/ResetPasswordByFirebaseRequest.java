package org.example.audio_ecommerce.dto.request;

import lombok.*;

@Getter
@Setter
public class ResetPasswordByFirebaseRequest {
    private String firebaseIdToken;
    private String newPassword;
}

