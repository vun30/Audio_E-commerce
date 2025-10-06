package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private UUID id;
    private String fullName;
    private String userName;
    private String email;
    private String phoneNumber;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String avatarURL;
    private CustomerStatus status;
    private Boolean twoFactorEnabled;
    private KycStatus kycStatus;
    private LocalDateTime lastLogin;
    private Integer addressCount;
    private Integer loyaltyPoints;
    private LoyaltyLevel loyaltyLevel;
    private Integer voucherCount;
    private Integer orderCount;
    private Integer cancelCount;
    private Integer returnCount;
    private Integer unpaidOrderCount;
    private LocalDate lastOrderDate;
    private String preferredCategory;
}
