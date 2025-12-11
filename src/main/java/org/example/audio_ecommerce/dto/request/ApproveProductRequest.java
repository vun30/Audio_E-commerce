package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class ApproveProductRequest {
    private boolean approved;   // true = duyệt -> ACTIVE
    private String reason;      // lý do (optional nếu approved, required nếu reject)
}
