package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class UpdateStoreRequest {
    private String storeName;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    private String address;
    private String phoneNumber;
    private String email;
}
