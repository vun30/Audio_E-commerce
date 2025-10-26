package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Password can not blank")
    private String password;

}
