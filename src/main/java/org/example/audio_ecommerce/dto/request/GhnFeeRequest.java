package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class GhnFeeRequest {

    @Schema(example = "5", description = "Mã loại dịch vụ (2: hàng nhẹ, 5: hàng nặng)")
    private Integer service_type_id;

    @Schema(example = "3695", description = "Mã Quận/Huyện người gửi")
    private Integer from_district_id;

    @Schema(example = "90768", description = "Mã Phường/Xã người gửi")
    private String from_ward_code;

    @Schema(example = "3440", description = "Mã Quận/Huyện người nhận")
    private Integer to_district_id;

    @Schema(example = "13010", description = "Mã Phường/Xã người nhận")
    private String to_ward_code;

    @Schema(example = "30", description = "Chiều dài đơn hàng (cm)")
    private Integer length;

    @Schema(example = "40", description = "Chiều rộng đơn hàng (cm)")
    private Integer width;

    @Schema(example = "20", description = "Chiều cao đơn hàng (cm)")
    private Integer height;

    @Schema(example = "3000", description = "Khối lượng đơn hàng (gram)")
    private Integer weight;

    @Schema(example = "0", description = "Giá trị bảo hiểm (tối đa 5.000.000)")
    private Integer insurance_value;

    @Schema(example = "FE trả về null", description = "Mã giảm giá GHN (nếu có)")
    private String coupon;

    @Schema(description = "Danh sách sản phẩm (bắt buộc nếu là hàng nặng)")
    private List<FeeItem> items;

    @Data
    @Schema(name = "FeeItem", description = "Thông tin từng sản phẩm gửi GHN")
    public static class FeeItem {
        @Schema(example = "TEST1", description = "Tên sản phẩm")
        private String name;

        @Schema(example = "1", description = "Số lượng sản phẩm")
        private Integer quantity;

        @Schema(example = "200", description = "Chiều dài sản phẩm (cm)")
        private Integer length;

        @Schema(example = "200", description = "Chiều rộng sản phẩm (cm)")
        private Integer width;

        @Schema(example = "200", description = "Chiều cao sản phẩm (cm)")
        private Integer height;

        @Schema(example = "1000", description = "Trọng lượng sản phẩm (gram)")
        private Integer weight;
    }
}
