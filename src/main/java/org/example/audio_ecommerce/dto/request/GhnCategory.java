package org.example.audio_ecommerce.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnCategory {
    private String level1;  // Danh mục cấp 1 (VD: "Phụ kiện", "Audio")
    private String level2;  // (Tuỳ chọn)
    private String level3;  // (Tuỳ chọn)
}
