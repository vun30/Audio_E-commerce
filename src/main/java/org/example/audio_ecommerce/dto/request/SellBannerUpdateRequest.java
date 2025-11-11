package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellBannerUpdateRequest {
    private String title;                 // optional (chỉ cập nhật field nào gửi lên)
    private String description;
    private String bannerType;
    private Boolean active;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<SellBannerImageRequest> images; // replace toàn bộ list ảnh
}
