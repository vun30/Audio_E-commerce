package org.example.audio_ecommerce.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnCreateOrderRequest {

    // ================== 🏷️ Thông tin thanh toán & ghi chú ==================
    private Integer payment_type_id;  // 1: Shop trả phí ship, 2: Người nhận trả
    private String note;              // Ghi chú đơn hàng
    private String required_note;     // KHONGCHOXEMHANG | CHOXEMHANGKHONGTHU | CHOTHUHANG

    // ================== 🏢 Thông tin người gửi ==================
    private String from_name;
    private String from_phone;
    private String from_address;
    private String from_ward_name;
    private String from_district_name;
    private String from_province_name;

    // ================== 🔁 Thông tin trả hàng ==================
    private String return_phone;
    private String return_address;
    private Integer return_district_id;
    private String return_ward_code;

    // ================== 📦 Thông tin người nhận ==================
    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private Integer to_district_id;

    // ================== 💰 Giá trị đơn hàng ==================
    private Integer cod_amount;        // Số tiền thu hộ (COD)
    private String content;            // Nội dung đơn hàng (ghi chú vận chuyển)

    // ================== ⚖️ Thông tin kích thước & trọng lượng ==================
    private Integer weight;            // gram
    private Integer length;            // cm
    private Integer width;             // cm
    private Integer height;            // cm

    // ================== 🚚 Thông tin dịch vụ ==================
    private Integer pick_station_id;   // Điểm lấy hàng (nếu >0)
    private Integer insurance_value;   // Giá trị bảo hiểm (tối đa 5.000.000)
    private Integer service_id;        // Mã dịch vụ
    private Integer service_type_id;   // 2: Ecommerce, 5: Traditional
    private String coupon;             // Mã giảm giá GHN
    private List<Integer> pick_shift;  // Ca lấy hàng (ví dụ [2])

    // ================== 🛍️ Danh sách sản phẩm ==================
    private List<GhnItem> items;
}
