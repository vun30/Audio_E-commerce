package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sell_banner_images")
public class SellBannerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", nullable = false)
    private SellBanner banner;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String imageUrl; // URL ảnh

    @Column(length = 500)
    private String redirectUrl; // URL link khi người dùng click vào ảnh

    @Column(length = 255)
    private String altText; // (Optional) text thay thế cho SEO

    @Column
    private Integer sortOrder = 0; // Thứ tự hiển thị (0,1,2,...)
}
