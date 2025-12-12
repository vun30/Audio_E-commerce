package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutGroup {

    private int countItems;            // số lượng order item
    private BigDecimal totalAmount;    // tổng tiền nhóm
    private List<PayoutItemDetail> items; // list các item cấu thành
}
