package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 🚚 ShippingMethod Entity — Đại diện cho phương thức vận chuyển (GHN, GHTK, ViettelPost, ...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "shipping_methods")
public class ShippingMethod {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "shipping_method_id", columnDefinition = "CHAR(36)")
    private UUID shippingMethodId;

    @Column(nullable = false, unique = true)
    private String name; // note: Tên đơn vị vận chuyển (VD: Giao Hàng Nhanh, GHTK)

    private String code; // note: Mã rút gọn (VD: GHN, GHTK)

    private String logoUrl; // note: Link logo hiển thị

    private BigDecimal baseFee; // note: Phí cơ bản (VD: 25000)

    private BigDecimal feePerKg; // note: Phí cộng thêm mỗi kg (VD: 5000)

    private Integer estimatedDeliveryDays; // note: Thời gian giao dự kiến (VD: 2 ngày)

    private Boolean supportCOD; // note: Có hỗ trợ COD không

    private Boolean supportInsurance; // note: Có bảo hiểm không

    private Boolean isActive; // note: Trạng thái kích hoạt

    private String description; // note: Mô tả thêm

    private String contactPhone; // note: SĐT CSKH

    private String websiteUrl; // note: Trang web chính thức
}
