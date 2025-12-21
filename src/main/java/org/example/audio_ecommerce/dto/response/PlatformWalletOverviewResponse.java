package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "PlatformWalletOverviewResponse",
    description = "Thông tin tổng quan ví nền tảng - bao gồm tổng tiền nạp, chi, hold, v.v"
)
public class PlatformWalletOverviewResponse {

    @Schema(
        description = "Tổng tiền khách hàng đã nạp vào platform",
        example = "50000000",
        type = "string"
    )
    private BigDecimal totalCustomerDeposit;  // ✅ Tổng tiền customer nạp

    @Schema(
        description = "Tổng tiền đang giữ hộ (hold 7 ngày từ orders)",
        example = "10000000",
        type = "string"
    )
    private BigDecimal pendingBalance;  // Tiền đang hold

    @Schema(
        description = "Tổng tiền đã settlement (hoàn tất)",
        example = "35000000",
        type = "string"
    )
    private BigDecimal doneBalance;  // Tiền đã done

    @Schema(
        description = "Tổng tiền đã hoàn trả cho khách hàng",
        example = "5000000",
        type = "string"
    )
    private BigDecimal refundedTotal;  // Tiền hoàn lại

    @Schema(
        description = "Tiền phí nền tảng đã thu",
        example = "2500000",
        type = "string"
    )
    private BigDecimal commissionBalance;  // Tiền phí platform

    @Schema(
        description = "Tiền thực tế platform đang giữ (cash)",
        example = "48000000",
        type = "string"
    )
    private BigDecimal cashBalance;  // Tiền cash thực tế

    @Schema(
        description = "Tổng số dư hiện tại",
        example = "48000000",
        type = "string"
    )
    private BigDecimal totalBalance;  // Tổng balance hiện tại

    @Schema(
        description = "Số lượng order đang pending (hold)",
        example = "150"
    )
    private Long pendingOrderCount;

    @Schema(
        description = "Số lượng order đã done",
        example = "1200"
    )
    private Long doneOrderCount;

    @Schema(
        description = "Thời gian cập nhật cuối",
        example = "2025-12-18T10:30:00"
    )
    private LocalDateTime lastUpdatedAt;

    @Schema(
        description = "Mô tả ngắn gọn trạng thái ví",
        example = "Platform wallet healthy: $48M cash, $10M pending"
    )
    private String summary;
}

