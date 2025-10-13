package org.example.audio_ecommerce.email.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycApprovedData {
    private String email;
    private String ownerName;
    private String storeName;
    private String siteUrl;
}
