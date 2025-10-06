package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UpdateStoreResponse {
    private UUID storeId;
    private String storeName;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    private String address;
    private String phoneNumber;
    private String email;
}
