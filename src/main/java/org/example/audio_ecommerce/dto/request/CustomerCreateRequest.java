package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.audio_ecommerce.entity.Enum.Gender;

import java.time.LocalDate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreateRequest {
    @NotBlank private String fullName;
    @NotBlank private String userName;
    @Email @NotBlank private String email;
    private String phoneNumber;
    @NotBlank private String password;     // sẽ encode -> passwordHash
    private Gender gender;                 // nullable
    private LocalDate dateOfBirth;
    private String avatarURL;
    private Boolean twoFactorEnabled;      // default xử lý ở service
    private String preferredCategory;
}
