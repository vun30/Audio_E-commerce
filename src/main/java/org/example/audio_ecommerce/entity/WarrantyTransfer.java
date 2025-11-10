package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="warranty_transfer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarrantyTransfer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="warranty_id", nullable=false)
    private Warranty warranty;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="from_customer_id", nullable=false)
    private Customer fromCustomer;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="to_customer_id", nullable=false)
    private Customer toCustomer;

    private LocalDateTime transferAt;
    @Lob private String note;

    @PrePersist public void pre(){ transferAt = LocalDateTime.now(); }
}

