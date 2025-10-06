package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    private UUID storeId;
    private String storeName;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    private String address;
    private String phoneNumber;
    private String email;
    private BigDecimal rating;
    private StoreStatus status;
    private UUID accountId;
}
