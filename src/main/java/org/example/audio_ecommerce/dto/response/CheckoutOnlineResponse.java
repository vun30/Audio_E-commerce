// dto/response/CheckoutOnlineResponse.java
package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CheckoutOnlineResponse {
    private UUID customerOrderId;
    private BigDecimal amount;
    private Long payOSOrderCode;
    private String checkoutUrl;     // FE mở link này
    private String qrCode;          // tuỳ PayOS trả
    private String status;          // AWAITING_PAYMENT | ERROR
}
