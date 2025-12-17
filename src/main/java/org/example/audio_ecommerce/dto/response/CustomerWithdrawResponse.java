package org.example.audio_ecommerce.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class CustomerWithdrawResponse {
    private UUID id;
    private UUID customerId;
    private BigDecimal amount;
    private WithdrawRequestStatus status;

    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;

    private String adminNote;
    private String payoutRef;

    private List<String> proofUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // lastUpdated
}
