package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyLogSearchRequest {

    private UUID warrantyId;               // bắt buộc
    private UUID customerId;              // optional – filter theo customer
    private UUID storeId;                 // optional – filter theo store
    private WarrantyLogStatus status;     // optional – OPEN, COMPLETED, ...
}
