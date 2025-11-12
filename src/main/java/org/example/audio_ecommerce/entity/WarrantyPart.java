package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="warranty_part")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarrantyPart {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="log_id", nullable=false)
    private LogWarranty log;

    @Column(length=64) private String partCode;
    @Column(length=255) private String partName;
    private int qty;

    @Column(precision=18, scale=2) private BigDecimal unitPrice = BigDecimal.ZERO;

    private boolean covered = true;

    @Lob private String note;
}

