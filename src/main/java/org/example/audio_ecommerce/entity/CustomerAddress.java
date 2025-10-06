package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.AddressLabel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(name = "idx_addr_customer", columnList = "customer_id"),
                @Index(name = "idx_addr_is_default", columnList = "is_default")
        }
)
public class CustomerAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, columnDefinition = "CHAR(36)")
    @JsonIgnore
    private Customer customer;

    // Người nhận & liên hệ
    @NotBlank
    @Column(name = "receiver_name", length = 255, nullable = false)
    private String receiverName;

    @NotBlank
    @Column(name = "phone_number", length = 30, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", length = 10)
    private AddressLabel label; // HOME/WORK/OTHER (tuỳ chọn)

    // Địa chỉ chi tiết (Việt Nam)
    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "province", length = 120)   // Tỉnh/TP
    private String province;

    @Column(name = "district", length = 120)   // Quận/Huyện
    private String district;

    @Column(name = "ward", length = 120)       // Phường/Xã
    private String ward;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "address_line", length = 512) // dòng địa chỉ gộp nếu muốn
    private String addressLine;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "note", length = 512)
    private String note;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    // Gợi ý: nếu cần toạ độ
    // @Column(name = "lat") private Double lat;
    // @Column(name = "lng") private Double lng;
}
