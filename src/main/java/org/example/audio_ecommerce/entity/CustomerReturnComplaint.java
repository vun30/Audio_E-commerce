package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ReturnComplaintStatus;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customer_return_complaints")
public class CustomerReturnComplaint extends BaseEntity {

    @Column(name = "return_request_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID returnRequestId;

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

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "customer_video_url", length = 512)
    private String customerVideoUrl;

    @ElementCollection
    @CollectionTable(
            name = "customer_return_complaint_images",
            joinColumns = @JoinColumn(name = "complaint_id")
    )
    @Column(name = "image_url", length = 512)
    private List<String> customerImageUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", length = 20)
    private ReturnReasonType reasonType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReturnComplaintStatus status;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "auto_refund_executed")
    private Boolean autoRefundExecuted;
}
