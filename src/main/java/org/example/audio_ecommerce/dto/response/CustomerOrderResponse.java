package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class CustomerOrderResponse {
    private UUID id;
    private String status;
    private String message;
    private String createdAt;
    private BigDecimal totalAmount;

    // ✅ Tổng giảm (voucher các shop)
    private BigDecimal discountTotal;

    // ✅ Số tiền phải trả sau giảm
    private BigDecimal grandTotal;


    // (tuỳ chọn) breakdown giảm theo từng shop: storeId -> discount
    private Map<UUID, BigDecimal> storeDiscounts;
    // ✅ breakdown giảm theo voucher toàn sàn: mã voucher -> số tiền giảm
    private Map<String, BigDecimal> platformDiscount;
    private String receiverName;
    private String phoneNumber;
    private String country;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private String postalCode;
    private String note;
    // Thêm các trường khác nếu cần
}

