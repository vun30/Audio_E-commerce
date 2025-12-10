package org.example.audio_ecommerce.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusChangedData {
    private String email;
    private String ownerName;
    private String storeName;
    private String newStatus;
    private String reason;     // có thể null
    private String siteUrl;    // link đăng nhập merchant
}
