package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CheckoutOnlineRequest {
    private UUID addressId;                // ưu tiên dùng như COD
    private String message;               // ghi chú
    private String description;           // mô tả đơn hàng
    private List<CheckoutItemRequest> items; // danh sách item được chọn trong giỏ
    private String returnUrl;             // FE gửi vào (ví dụ https://dats.vn/checkout/success)
    private String cancelUrl;             // FE gửi vào (ví dụ https://dats.vn/checkout/cancel)
}
