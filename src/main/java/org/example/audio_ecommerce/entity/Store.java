package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "store_id", columnDefinition = "CHAR(36)")
    private UUID storeId; // 🔹 Mã cửa hàng

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, columnDefinition = "CHAR(36)")
    @JsonIgnore
    private Account account; // 🔹 Liên kết với Account

    // ✅ Liên kết 1-1 với StoreWallet (thay vì lưu walletId thủ công)
    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private StoreWallet wallet;

    @Column(nullable = false, length = 255)
    private String storeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;
    private String coverImageUrl;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String email;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StoreStatus status;

    private LocalDateTime createdAt;

    // ========================
    // 🔹 Quan hệ với Product
    // ========================
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Product> products;
}
