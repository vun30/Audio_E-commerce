package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SimilarProductRequest {
    private UUID categoryId;            // bắt buộc để lọc nhanh
    private Integer topN = 20;
    private ThongSoKyThuatDto thongSoKyThuat;
}
