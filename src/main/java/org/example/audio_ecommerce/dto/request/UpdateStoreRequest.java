package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreRequest {

    @Schema(description = "Tên cửa hàng", example = "AudioPro Store")
    private String storeName;

    @Schema(description = "Mô tả cửa hàng", example = "Chuyên cung cấp thiết bị âm thanh cao cấp")
    private String description;

    @Schema(description = "URL logo cửa hàng", example = "https://cdn.example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Ảnh bìa cửa hàng", example = "https://cdn.example.com/cover.jpg")
    private String coverImageUrl;

    @Schema(description = "Địa chỉ hiển thị bên ngoài (không phải kho chi nhánh)",
            example = "123 Nguyễn Trãi, Hà Nội")
    private String address;

    @Schema(description = "Số điện thoại cửa hàng", example = "0987654321")
    private String phoneNumber;

    @Schema(description = "Email cửa hàng", example = "audiopro@gmail.com")
    private String email;

    // =========================================================
    // 🏢 DANH SÁCH ĐỊA CHỈ CHI NHÁNH / KHO CỦA CỬA HÀNG
    // =========================================================
    @Schema(description = "Danh sách địa chỉ chi nhánh/kho sẽ được cập nhật. " +
            "Nếu addressId = null → tạo mới. Nếu có addressId → update địa chỉ đó.")
    private List<StoreAddressRequest> storeAddresses;

    // ================================
    // DTO ADDRESS REQUEST
    // ================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreAddressRequest {

        @Schema(description = "ID của địa chỉ (null nếu thêm mới)",
                example = "6a3a2e78-1710-4571-9239-c23c7aaf9012")
        private UUID addressId;

        @Schema(description = "Đánh dấu địa chỉ mặc định", example = "true")
        private Boolean defaultAddress;

        @Schema(description = "Mã tỉnh/thành phố", example = "01")
        private String provinceCode;

        @Schema(description = "Mã quận/huyện", example = "760")
        private String districtCode;

        @Schema(description = "Mã phường/xã", example = "26734")
        private String wardCode;

        @Schema(description = "Địa chỉ chi tiết", example = "123 Nguyễn Trãi, Quận 1, TP.HCM")
        private String address;

        @Schema(description = "Toạ độ GPS", example = "10.776530,106.700981")
        private String addressLocation;
    }
}
