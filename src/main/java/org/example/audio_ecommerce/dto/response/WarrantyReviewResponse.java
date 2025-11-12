package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ⭐ Đánh giá sau bảo hành
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class WarrantyReviewResponse {

    private UUID id;                 // ID review
    private UUID warrantyId;         // Gắn với bảo hành
    private UUID logId;              // Gắn với ticket
    private UUID customerId;         // Người đánh giá
    private String customerName;     // Tên khách hàng
    private Integer rating;          // Điểm (1–5)
    private String comment;          // Nhận xét
    private LocalDateTime createdAt; // Thời điểm gửi
}
