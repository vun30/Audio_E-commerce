package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionStatus;
import org.example.audio_ecommerce.entity.Enum.WalletTransactionType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_txn_wallet", columnList = "wallet_id"),
                @Index(name = "idx_txn_order", columnList = "order_id"),
                @Index(name = "idx_txn_created", columnList = "created_at")
        }
)
public class WalletTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, columnDefinition = "CHAR(36)")
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 12, nullable = false)
    private WalletTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private WalletTransactionStatus status;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "balance_before", precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    // Optional: liên kết đơn hàng
    @Column(name = "order_id", columnDefinition = "CHAR(36)")
    private UUID orderId;
}
