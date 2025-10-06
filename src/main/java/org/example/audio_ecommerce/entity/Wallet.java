package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallet_customer", columnNames = "customer_id"),
        indexes = @Index(name = "idx_wallet_customer", columnList = "customer_id")
)
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private Customer customer;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;
}
