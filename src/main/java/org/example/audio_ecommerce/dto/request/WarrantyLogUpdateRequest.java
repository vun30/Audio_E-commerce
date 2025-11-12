package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WarrantyLogUpdateRequest {
    private String diagnosis;
    private String resolution;
    private String shipBackTracking;
    private List<String> attachmentUrls;
    private BigDecimal costLabor;
    private BigDecimal costParts;
}
