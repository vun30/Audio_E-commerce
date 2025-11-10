package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.WarrantyLogStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name="log_warranty",
        indexes = {@Index(name="idx_log_warranty_status", columnList="status")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LogWarranty {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="warranty_id", nullable=false)
    private Warranty warranty;

    @Enumerated(EnumType.STRING) @Column(length=20, nullable=false)
    private WarrantyLogStatus status;

    private LocalDateTime openedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    @Lob private String problemDescription;
    @Lob private String diagnosis;
    @Lob private String resolution;

    private Boolean covered; // null = theo policy mặc định

    @Column(precision=18, scale=2) private BigDecimal costLabor = BigDecimal.ZERO;
    @Column(precision=18, scale=2) private BigDecimal costParts = BigDecimal.ZERO;
    @Column(precision=18, scale=2) private BigDecimal costTotal = BigDecimal.ZERO;

    @Lob private String attachmentsJson;
    private String shipBackTracking;

    private LocalDateTime receivedAt;
    private LocalDateTime returnedAt;

    @OneToMany(mappedBy="log", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<WarrantyPart> parts = new ArrayList<>();

    @PrePersist
    public void onOpen() { openedAt = LocalDateTime.now(); updatedAt = openedAt; if(status==null) status = WarrantyLogStatus.OPEN; }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == WarrantyLogStatus.COMPLETED && closedAt == null) closedAt = updatedAt;
        if (Boolean.FALSE.equals(covered)) {
            if (costLabor==null) costLabor = BigDecimal.ZERO;
            if (costParts==null) costParts = BigDecimal.ZERO;
            costTotal = costLabor.add(costParts);
        } else {
            costTotal = BigDecimal.ZERO;
        }
    }
}

