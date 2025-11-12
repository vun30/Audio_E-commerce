package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WarrantyStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warranty",
        indexes = {
                @Index(name="idx_warranty_serial", columnList="serial_number"),
                @Index(name="idx_warranty_customer", columnList="customer_id"),
                @Index(name="idx_warranty_end_date", columnList="end_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name="uk_warranty_serial_product",
                        columnNames={"serial_number","product_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Warranty {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="customer_id", nullable=false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="store_id", nullable=false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @Column(name="store_order_item_id", columnDefinition="CHAR(36)")
    private UUID storeOrderItemId; // tham chiếu mềm

    @Column(name="serial_number", length=128)
    private String serialNumber;

    @Column(name="policy_code", length=64)
    private String policyCode;

    @Column(name="duration_months", nullable=false)
    private Integer durationMonths;

    private LocalDate purchaseDate;
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable=false) @Builder.Default
    private boolean covered = true;

    @Enumerated(EnumType.STRING)
    @Column(length=20, nullable=false)
    @Builder.Default
    private WarrantyStatus status = WarrantyStatus.ACTIVE;

    private LocalDateTime activatedAt;

    @Lob private String notes;

    @Column(name="latest_ticket_id", columnDefinition="CHAR(36)")
    private UUID latestTicketId;

    @PrePersist
    public void prePersist() {
        if (startDate == null && purchaseDate != null) startDate = purchaseDate;
        if (endDate == null && startDate != null && durationMonths > 0)
            endDate = startDate.plusMonths(durationMonths);
        if (activatedAt == null) activatedAt = LocalDateTime.now();
    }
}

