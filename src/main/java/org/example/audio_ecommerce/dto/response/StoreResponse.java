package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    @Schema(description = "ID cửa hàng", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID storeId;

    @Schema(description = "Tên cửa hàng", example = "AudioPro Store")
    private String storeName;

    @Schema(description = "Mô tả cửa hàng", example = "Cung cấp thiết bị âm thanh cao cấp")
    private String description;

    @Schema(description = "URL logo cửa hàng", example = "https://cdn.example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Ảnh bìa của cửa hàng", example = "https://cdn.example.com/cover.jpg")
    private String coverImageUrl;

    @Schema(description = "Địa chỉ hiển thị bên ngoài", example = "123 Nguyễn Trãi, Hà Nội")
    private String address;

    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    private String phoneNumber;

    @Schema(description = "Email cửa hàng", example = "audiopro@gmail.com")
    private String email;

    @Schema(description = "Điểm đánh giá trung bình của cửa hàng", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "Trạng thái cửa hàng", example = "ACTIVE")
    private StoreStatus status;

    @Schema(description = "ID tài khoản chủ cửa hàng", example = "0a1b2c3d-4e5f-6789-abcd-ef0123456789")
    private UUID accountId;

    // =========================================================
    // 🏢 DANH SÁCH ĐỊA CHỈ CHI NHÁNH / KHO CỦA CỬA HÀNG
    // =========================================================
    @Schema(description = "Danh sách địa chỉ chi nhánh/kho của cửa hàng")
    private List<StoreAddressResponse> storeAddresses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreAddressResponse {

        @Schema(description = "ID địa chỉ chi nhánh/kho",
                example = "6a3a2e78-1710-4571-9239-c23c7aaf9012")
        private UUID addressId;

        @Schema(description = "Địa chỉ mặc định hay không", example = "true")
        private Boolean defaultAddress;

        @Schema(description = "Mã tỉnh/thành phố", example = "01")
        private String provinceCode;

        @Schema(description = "Mã quận/huyện", example = "760")
        private String districtCode;

        @Schema(description = "Mã phường/xã", example = "26734")
        private String wardCode;

        @Schema(description = "Địa chỉ chi tiết", example = "123 Nguyễn Trãi, Quận 1, TP.HCM")
        private String address;

        @Schema(description = "Tọa độ hoặc vị trí GPS", example = "10.776530,106.700981")
        private String addressLocation;
    }
}
