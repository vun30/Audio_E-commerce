package org.example.audio_ecommerce.dto.response;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ReturnFaultType;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReturnRequestResponse {

    private UUID id;
    private UUID customerId;
    private UUID shopId;
    private UUID orderItemId;
    private UUID productId;
    private String productName;
    private BigDecimal itemPrice;
    private UUID escalatedById;   // shopId
    private String escalatedByName; // storeName
    private String escalatedByRole;
    // ===== NEW =====
    private String customerName;
    private String storeName;
    private BigDecimal customerLegalPoint;
    private BigDecimal storeLegalPoint;
    private String orderCode;
    // =================

    private ReturnReasonType reasonType;
    private String reason;

    private List<String> customerImageUrls;
    private String customerVideoUrl;

    private ReturnStatus status;
    private ReturnFaultType faultType;

    private BigDecimal packageWeight;
    private BigDecimal packageLength;
    private BigDecimal packageWidth;
    private BigDecimal packageHeight;
    private BigDecimal shippingFee;

    private String ghnOrderCode;
    private String trackingStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean finalDecision;
    private boolean adminForcedContinue;
}

