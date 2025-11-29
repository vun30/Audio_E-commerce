package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class CustomerOrderResponse {
    private UUID id;
    private String orderCode;
    private String status;
    private String message;
    private String createdAt;

    // ===== NEW: định danh shop của đơn này =====
    private UUID storeId;
    private String storeName;

    // ===== Tổng tiền của RIÊNG shop này =====
    private BigDecimal totalAmount;

    // ===== NEW: phí ship của RIÊNG shop này =====
    private BigDecimal shippingFeeTotal;

    // Tổng giảm (shop-voucher + platform-voucher) của RIÊNG shop này
    private BigDecimal discountTotal;

    // Số tiền phải trả sau giảm cho RIÊNG shop này
    private BigDecimal grandTotal;

    // (tuỳ chọn) breakdown giảm theo shop-voucher: code -> amount (nếu muốn giữ chi tiết theo từng mã)
    private Map<String, BigDecimal> storeVoucherDiscount;

    // breakdown giảm theo voucher toàn sàn: mã voucher -> số tiền giảm (của RIÊNG shop này)
    private Map<String, BigDecimal> platformDiscount;

    // Shipping snapshot (địa chỉ nhận)
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

    // ===== NEW: service type GHN đã chọn cho shop này (2 | 5), tiện hiển thị/trace =====
    private Integer shippingServiceTypeId;
}
