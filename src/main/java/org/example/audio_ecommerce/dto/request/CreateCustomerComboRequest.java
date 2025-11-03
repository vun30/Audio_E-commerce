package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCustomerComboRequest {
    private UUID customerId;
    private UUID storeId;

    private String name;
    private String shortDescription;
    private String description;
    private List<String> images;
    private String videoUrl;

    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;
    private String provinceCode;
    private String districtCode;
    private String wardCode;

    private List<ComboItemRequest> items;
    private UUID createdBy; // accountId thao tác
}
