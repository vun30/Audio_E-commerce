package org.example.audio_ecommerce.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnItem {
    private String name;         // Tên sản phẩm
    private String code;         // Mã sản phẩm
    private Integer quantity;    // Số lượng
    private Integer price;       // Giá
    private Integer length;      // Chiều dài (cm)
    private Integer width;       // Chiều rộng (cm)
    private Integer height;      // Chiều cao (cm)
    private Integer weight;      // Trọng lượng (gram)
    private GhnCategory category; // Phân loại sản phẩm
}
