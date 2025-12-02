package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusRatioResponse {
    private double pendingRatio;
    private double processingRatio;
    private double shippingRatio;
    private double completedRatio;
    private double cancelledRatio;
    private double returnedRatio;
}

