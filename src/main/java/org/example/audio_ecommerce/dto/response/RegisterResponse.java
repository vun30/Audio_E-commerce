package org.example.audio_ecommerce.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterResponse {
    private String name;
    private String email;
}
