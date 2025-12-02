package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class RegisterDeviceTokenRequest {
    private String token;
    private String platform; // ANDROID / IOS / WEB
}
