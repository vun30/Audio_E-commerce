package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "store_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAddressEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "address_id", columnDefinition = "CHAR(36)")
    private UUID id;

    // 🔗 MANY-TO-ONE → một Store có nhiều địa chỉ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, columnDefinition = "CHAR(36)")
    @JsonBackReference
    private Store store;

    private Boolean defaultAddress;   // Địa chỉ mặc định
    private String provinceCode;      // Mã tỉnh
    private String districtCode;      // Mã quận
    private String wardCode;          // Mã phường
    @Column(length = 500)
    private String address;           // Địa chỉ chi tiết
    private String addressLocation;   // Tọa độ
}
