package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReturnPackageInfoRequest {

    @NotNull
    private BigDecimal weight;

    @NotNull
    private BigDecimal length;

    @NotNull
    private BigDecimal width;

    @NotNull
    private BigDecimal height;

    // ✅ Customer chọn địa chỉ nhận GHN tới (pickup tại nhà customer)
    private UUID customerAddressId;   // optional, nếu null → dùng default

    // ✅ (Optional) Shop chọn địa chỉ kho nhận hàng
    private UUID storeAddressId;      // optional, nếu null → dùng default store address
}
