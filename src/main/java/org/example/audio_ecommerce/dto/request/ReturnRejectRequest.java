package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRejectRequest {
    // Lý do shop không chấp nhận hoàn hàng (optional)
    private String shopRejectReason;
}
