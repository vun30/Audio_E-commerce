package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "withdraw_proofs",
        indexes = {
                @Index(name = "idx_wp_withdraw", columnList = "withdraw_request_id")
        }
)
public class WithdrawProof extends BaseEntity {
    //entity này để lưu các bằng chứng admin chuyển tiền như hình ảnh hóa đơn, file pdf, v.v.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "withdraw_request_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID withdrawRequestId;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(length = 255)
    private String note;// admin
}
