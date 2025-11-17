package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreResponse {

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

    @Schema(description = "Địa chỉ chính của cửa hàng", example = "123 Nguyễn Trãi, Hà Nội")
    private String address;

    @Schema(description = "Số điện thoại cửa hàng", example = "0987654321")
    private String phoneNumber;

    @Schema(description = "Email cửa hàng", example = "audiopro@gmail.com")
    private String email;

    @Schema(description = "Danh sách địa chỉ chi nhánh/kho sau khi cập nhật")
    private List<StoreAddressResponse> storeAddresses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreAddressResponse {

        @Schema(description = "ID địa chỉ", example = "8b8b0a74-1c5e-4f8e-9a0b-123456789abc")
        private UUID addressId;

        @Schema(description = "Đây có phải địa chỉ mặc định không", example = "true")
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
