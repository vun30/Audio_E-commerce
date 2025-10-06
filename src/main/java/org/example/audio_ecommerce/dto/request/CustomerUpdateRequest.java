package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUpdateRequest {
    private String fullName;
    private String userName;
    @Email private String email;
    private String phoneNumber;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String avatarURL;
    private CustomerStatus status;
    private Boolean twoFactorEnabled;
    private KycStatus kycStatus;
    private String preferredCategory;
    private Integer loyaltyPoints;
    private LoyaltyLevel loyaltyLevel;
}
