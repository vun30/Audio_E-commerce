package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delivery_location_log",
        indexes = {
                @Index(name = "idx_assignment_time", columnList = "delivery_assignment_id, logged_at"),
                @Index(name = "idx_store_order_time", columnList = "store_order_id, logged_at")
        })
public class DeliveryLocationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // tiện truy vấn nhanh theo đơn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_assignment_id", nullable = false)
    private DeliveryAssignment assignment;

    private Double latitude;               // WGS84
    private Double longitude;
    private Double speedKmh;               // tùy chọn
    private LocalDateTime loggedAt;        // server time
    @Column(length = 512)
    private String addressText;            // reverse geocoding client-side (optional)
}
