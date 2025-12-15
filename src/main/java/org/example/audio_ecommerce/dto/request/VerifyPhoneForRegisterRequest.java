package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;

@Getter
@Setter
public class VerifyPhoneForRegisterRequest {
    private String firebaseIdToken;
    private RoleEnum role; // CUSTOMER / STOREOWNER / ADMIN...
}

