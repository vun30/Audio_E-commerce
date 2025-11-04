package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "delivery_assignment")
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Đơn của store
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    // Staff chuẩn bị hàng (có thể chính là staffDeliver nếu cửa hàng nhỏ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by_staff_id") // Staff.id
    private Staff preparedBy;

    // Staff giao hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_staff_id", nullable = false)
    private Staff deliveryStaff;

    private LocalDateTime assignedAt;      // thời điểm phân công
    private LocalDateTime pickUpAt;        // thời điểm nhận hàng rời kho
    private LocalDateTime deliveredAt;     // thời điểm giao đến địa chỉ

    @Column(length = 1024)
    private String note;                   // ghi chú giao hàng (ví dụ: giao trong giờ HC)
}
