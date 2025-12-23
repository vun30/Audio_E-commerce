package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendVerifyEmailRequest {

    @Email
    @NotBlank
    private String email;

    private RoleEnum role; // optional: nếu bạn muốn xác định đúng email+role
}
