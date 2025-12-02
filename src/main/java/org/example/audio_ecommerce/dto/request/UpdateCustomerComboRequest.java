package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerComboRequest {

    // ❌ FE không cần gửi customerId → BE tự lấy từ token
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Media
    private List<String> images;
    private String videoUrl;

    // ⚖️ Logistics
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;
    private String provinceCode;
    private String districtCode;
    private String wardCode;

    // 🔥 Trạng thái combo
    private Boolean isActive;

    // 🧩 Danh sách item mới (full variant info)
    private List<ComboItemRequest> items;

    // ❌ updatedBy → BE tự set trong service
}
