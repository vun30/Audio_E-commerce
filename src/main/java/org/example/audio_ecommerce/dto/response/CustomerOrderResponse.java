package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CustomerOrderResponse {
    private UUID id;
    private String status;
    private String message;
    private String createdAt;
    private BigDecimal totalAmount;
    private String receiverName;
    private String phoneNumber;
    private String country;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private String postalCode;
    private String note;
    // Thêm các trường khác nếu cần
}

