package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    private BigDecimal amount;     // số tiền rút
    private String bankName;       // optional
    private String bankAccountNo;  // optional
    private String bankAccountName;// optional
    private String note;           // optional
}
