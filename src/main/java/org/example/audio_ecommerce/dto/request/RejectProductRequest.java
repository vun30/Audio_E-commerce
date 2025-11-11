package org.example.audio_ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectProductRequest {
    private List<UUID> campaignProductIds;

    // nếu từ chối nhiều thì dùng chung reason này
    private String reason;

    // tùy chọn: nếu FE muốn gửi từng lý do riêng cho từng product
    // ví dụ: { "reasonMap": { "id1": "Lý do A", "id2": "Lý do B" } }
    private Map<UUID, String> reasonMap;
}
