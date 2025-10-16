package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingMethodRequest {
    private String name;
    private String code;
    private String logoUrl;
    private BigDecimal baseFee;
    private BigDecimal feePerKg;
    private Integer estimatedDeliveryDays;
    private Boolean supportCOD;
    private Boolean supportInsurance;
    private Boolean isActive;
    private String description;
    private String contactPhone;
    private String websiteUrl;
}
