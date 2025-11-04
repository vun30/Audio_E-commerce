package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delivery_proof")
public class DeliveryProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_staff_id", nullable = false)
    private Staff deliveryStaff;

    @Column(length = 512)
    private String photoUrl;           // ảnh giao hàng/lắp đặt

    private Boolean installed;         // có lắp đặt tại chỗ không
    @Column(length = 1024)
    private String note;               // ghi chú

    private LocalDateTime createdAt;
}
