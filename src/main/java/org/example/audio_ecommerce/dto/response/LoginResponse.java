package org.example.audio_ecommerce.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer ";
    private AccountResponse user;
    private StaffInfo staff;

    public LoginResponse(String accessToken, String refreshToken, AccountResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public LoginResponse(String accessToken, String refreshToken, AccountResponse user, StaffInfo staff) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.staff = staff;
    }
}
