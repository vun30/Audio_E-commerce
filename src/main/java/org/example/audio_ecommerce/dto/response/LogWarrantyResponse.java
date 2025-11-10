package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 🧾 Phiếu bảo hành / ticket — dùng cho mở & theo dõi xử lý bảo hành
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LogWarrantyResponse {

    private UUID id;                     // ID ticket
    private UUID warrantyId;             // Gắn với bảo hành nào
    private String status;               // OPEN / DIAGNOSING / REPAIRING / COMPLETED / CLOSED
    private String problemDescription;   // Mô tả lỗi khách khai
    private String diagnosis;            // Chuẩn đoán kỹ thuật
    private String resolution;           // Hướng xử lý
    private Boolean covered;             // Được bảo hành miễn phí hay không

    private BigDecimal costLabor;        // Tiền công
    private BigDecimal costParts;        // Tiền linh kiện
    private BigDecimal costTotal;        // Tổng chi phí (nếu không covered)

    private List<String> attachmentUrls; // Ảnh/video biên bản
    private String shipBackTracking;     // Mã vận đơn trả hàng (nếu có)
    private LocalDateTime createdAt;     // Ngày tiếp nhận
    private LocalDateTime updatedAt;     // Cập nhật cuối
}
