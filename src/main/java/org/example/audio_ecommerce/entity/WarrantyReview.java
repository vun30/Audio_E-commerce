package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="warranty_review",
        uniqueConstraints = @UniqueConstraint(name="uk_review_log_customer", columnNames={"log_id","customer_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarrantyReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="warranty_id", nullable=false)
    private Warranty warranty;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="log_id", nullable=false)
    private LogWarranty log;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id", nullable=false)
    private Customer customer;

    private int rating; // 1..5
    @Lob private String comment;
    private LocalDateTime createdAt;
    @PrePersist public void pre(){ createdAt = LocalDateTime.now(); }
}

