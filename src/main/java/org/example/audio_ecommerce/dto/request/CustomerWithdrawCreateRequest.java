package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
public class CustomerWithdrawCreateRequest {
    private BigDecimal amount;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String note; // optional nice
}

