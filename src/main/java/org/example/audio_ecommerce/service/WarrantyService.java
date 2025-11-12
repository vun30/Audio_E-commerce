// service/WarrantyService.java
package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.LogWarrantyResponse;
import org.example.audio_ecommerce.dto.response.WarrantyResponse;
import org.example.audio_ecommerce.dto.response.WarrantyReviewResponse;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarrantyService {
    void activateForStoreOrder(UUID storeOrderId);
    Optional<WarrantyResponse> activateSingleItem(org.example.audio_ecommerce.entity.StoreOrderItem item);

    List<WarrantyResponse> search(WarrantySearchRequest req);

    LogWarrantyResponse openTicket(UUID warrantyId, WarrantyLogOpenRequest req);
    LogWarrantyResponse updateTicketStatus(UUID logId, WarrantyLogStatus newStatus, WarrantyLogUpdateRequest req);

    WarrantyResponse setSerialFirstTime(UUID warrantyId, String serial, String note);

    WarrantyReviewResponse review(UUID logId, UUID customerId, WarrantyReviewRequest req);
}
