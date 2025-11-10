package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 🛡️ Thông tin bảo hành — dùng cho tra cứu & kích hoạt serial
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class WarrantyResponse {

    private UUID id;                     // ID bảo hành
    private UUID productId;              // Sản phẩm được bảo hành
    private String productName;          // Tên sản phẩm
    private UUID storeId;                // Cửa hàng bán
    private String storeName;            // Tên cửa hàng
    private UUID customerId;             // Khách hàng sở hữu
    private String customerName;         // Tên khách hàng

    private String serialNumber;         // Số serial (nếu đã kích hoạt)
    private String policyCode;           // Mã chính sách
    private Integer durationMonths;      // Thời gian bảo hành (tháng)
    private LocalDate purchaseDate;      // Ngày mua hàng
    private LocalDate startDate;         // Ngày kích hoạt
    private LocalDate endDate;           // Ngày hết hạn
    private String status;               // ACTIVE / EXPIRED / VOID
    private Boolean covered;             // Có được bảo hành miễn phí không
    private boolean stillValid;
}
