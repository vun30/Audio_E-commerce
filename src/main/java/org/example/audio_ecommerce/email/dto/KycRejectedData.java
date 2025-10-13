package org.example.audio_ecommerce.email.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycRejectedData {
    private String email;
    private String ownerName;
    private String storeName;
    private String reason;
    private String siteUrl;
}
