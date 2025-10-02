package org.example.audio_ecommerce.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // ✅ thêm cái này để có constructor (String email, String name)
public class RegisterResponse {
    private String email;
    private String name;
}
