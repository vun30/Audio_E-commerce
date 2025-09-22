package org.example.audio_ecommerce.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer ";
    private AccountResponse user;

    public LoginResponse(String accessToken, AccountResponse user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}
