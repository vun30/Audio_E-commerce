package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CartStatus;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "carts",
        indexes = {
                @Index(name = "idx_cart_customer_active", columnList = "customer_id,status")
        })
public class Cart {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name="uuid2", strategy="uuid2")
    @Column(name = "cart_id", columnDefinition = "CHAR(36)")
    private UUID cartId;

    /** Mỗi customer chỉ có 1 cart ACTIVE */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // Tổng tiền
    @Builder.Default
    @Column(precision = 18, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2) private BigDecimal discountTotal = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2) private BigDecimal grandTotal = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = LocalDateTime.now(); }
}
