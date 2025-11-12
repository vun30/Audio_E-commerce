package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponse {
    private UUID id;

    // StoreOrder snapshot (mỏng)
    private UUID storeOrderId;
    private String orderStatus;
    private String shipReceiverName;
    private String shipPhoneNumber;

    // Staff
    private UUID deliveryStaffId;
    private String deliveryStaffName;

    private UUID preparedById;
    private String preparedByName;

    // Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime pickUpAt;
    private LocalDateTime deliveredAt;

    private String note;
    private BigDecimal orderTotal;
    private List<OrderItemResponse> items;
}
