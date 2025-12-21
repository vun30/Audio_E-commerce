package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTransactionResponse {

    private UUID id;

    // wallet + relation ids
    private UUID walletId;
    private UUID orderId;
    private UUID storeId;
    private UUID customerId;

    // core
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private PaymentChannel channel;
    private WalletBucket bucket;
    private TxDirection direction;

    // balance snapshot
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    // idempotency/external
    private String idempotencyKey;
    private String externalRefId;
    private String externalRefCode;

    // breakdown
    private BigDecimal itemAmount;
    private BigDecimal shipCustomerPaid;
    private BigDecimal shipReal;
    private BigDecimal shipDiffChargeStore;
    private BigDecimal commissionAmount;
    private BigDecimal commissionRate;

    // payout
    private UUID payoutRequestId;
    private BigDecimal payoutGross;
    private BigDecimal debtDeducted;
    private BigDecimal payoutNet;

    // misc
    private String description;
    private String metadataJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
