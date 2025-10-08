package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "store_wallets")
public class StoreWallet {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "wallet_id", columnDefinition = "CHAR(36)")
    private UUID walletId;

    // ✅ Liên kết ngược về Store
    @OneToOne
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StoreWalletTransaction> transactions;
}
