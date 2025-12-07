package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ReturnFaultType;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;
import org.example.audio_ecommerce.entity.Enum.ReturnStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "return_requests")
public class ReturnRequest extends BaseEntity {

    // ====== LIÊN KẾT ĐƠN HÀNG ======
    @Column(name = "customer_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID customerId;

    @Column(name = "shop_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID shopId;

    @Column(name = "order_item_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID orderItemId;

    @Column(name = "product_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "item_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal itemPrice;

    // ====== LÝ DO & MEDIA PHÍA KHÁCH ======
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 20)
    private ReturnReasonType reasonType;  // CUSTOMER_FAULT / SHOP_FAULT

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "customer_video_url", length = 512)
    private String customerVideoUrl;

    @ElementCollection
    @CollectionTable(
            name = "return_request_customer_images",
            joinColumns = @JoinColumn(name = "return_request_id")
    )
    @Column(name = "image_url", length = 512)
    private List<String> customerImageUrls;

    // ====== TRẠNG THÁI RETURN ======
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReturnStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fault_type", length = 20)
    private ReturnFaultType faultType;

    // ====== GÓI HÀNG ======
    @Column(name = "package_weight", precision = 18, scale = 2)
    private BigDecimal packageWeight;

    @Column(name = "package_length", precision = 18, scale = 2)
    private BigDecimal packageLength;

    @Column(name = "package_width", precision = 18, scale = 2)
    private BigDecimal packageWidth;

    @Column(name = "package_height", precision = 18, scale = 2)
    private BigDecimal packageHeight;

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    // ====== ĐỊA CHỈ PICKUP CUSTOMER ======
    @Column(name = "pickup_ward_code", length = 20)
    private String pickupWardCode;

    @Column(name = "pickup_ward_name", length = 100)
    private String pickupWardName;

    @Column(name = "pickup_district_code", length = 20)
    private String pickupDistrictCode;

    @Column(name = "pickup_district_name", length = 100)
    private String pickupDistrictName;

    @Column(name = "pickup_province_code", length = 20)
    private String pickupProvinceCode;

    @Column(name = "pickup_province_name", length = 100)
    private String pickupProvinceName;

    @Column(name = "pickup_address_line", length = 500)
    private String pickupAddressLine;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    // ====== GHN ======
    @Column(name = "ghn_order_code", length = 64)
    private String ghnOrderCode;

    @Column(name = "tracking_status", length = 64)
    private String trackingStatus;

    // ====== KHIẾU NẠI SHOP ======
    @Column(name = "shop_video_url", length = 512)
    private String shopVideoUrl;

    @ElementCollection
    @CollectionTable(
            name = "return_request_shop_images",
            joinColumns = @JoinColumn(name = "return_request_id")
    )
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private List<String> shopImageUrls;

    @Column(name = "shop_dispute_reason", length = 1000)
    private String shopDisputeReason;
}
