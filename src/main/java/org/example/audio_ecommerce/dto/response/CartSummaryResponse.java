package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CartSummaryResponse {
    private UUID cartId;
    private String status;                // ACTIVE/LOCKED/CHECKED_OUT
    private List<StoreGroup> groups;      // group theo shop
    private int selectedCount;            // tổng SL đã tick
    private BigDecimal selectedTotal;     // tổng tiền đã tick
}
