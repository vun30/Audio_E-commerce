package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "platform_transaction")
public class PlatformTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =========================
    // WALLET LINK
    // =========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private PlatformWallet wallet;

    // =========================
    // RELATION IDS (OPTIONAL)
    // =========================
    @Column
    private UUID orderId;

    @Column
    private UUID storeId;

    @Column
    private UUID customerId;

    // =========================
    // CORE LEDGER AMOUNT
    // =========================
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    // =========================
    // TRANSACTION META (BẮT BUỘC SET)
    // =========================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletBucket bucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TxDirection direction;

    // =========================
    // LEDGER BALANCE SNAPSHOT
    // =========================
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    // =========================
    // TIME
    // =========================
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // =========================
    // IDEMPOTENCY / EXTERNAL
    // =========================
    @Column(length = 80, unique = true)
    private String idempotencyKey;

    @Column(length = 120)
    private String externalRefId;

    @Column(length = 120)
    private String externalRefCode;

    // =========================
    // ORDER / COMMISSION BREAKDOWN
    // =========================
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal itemAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal shipCustomerPaid = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal shipReal = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal shipDiffChargeStore = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    // =========================
    // PAYOUT
    // =========================
    @Column
    private UUID payoutRequestId;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal payoutGross = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal debtDeducted = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal payoutNet = BigDecimal.ZERO;

    // =========================
    // MISC
    // =========================
    @Column(length = 255)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;
}
