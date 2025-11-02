package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderDetailResponse {
    private UUID id;
    private UUID storeId;
    private String storeName;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private BigDecimal discountTotal;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private UUID customerOrderId;
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    private String customerMessage;
    private String shipReceiverName;
    private String shipPhoneNumber;
    private String shipCountry;
    private String shipProvince;
    private String shipDistrict;
    private String shipWard;
    private String shipStreet;
    private String shipAddressLine;
    private String shipPostalCode;
    private String shipNote;
    private List<StoreOrderItemResponse> items;
}