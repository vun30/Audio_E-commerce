package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusCountsResponse {
    private long pending;
    private long processing; // CONFIRMED + AWAITING_SHIPMENT
    private long shipping;   // SHIPPING
    private long completed;  // COMPLETED
    private long cancelled;  // CANCELLED
    private long returned;   // RETURNED
    private long total;      // tổng tất cả (sum trên)
}

