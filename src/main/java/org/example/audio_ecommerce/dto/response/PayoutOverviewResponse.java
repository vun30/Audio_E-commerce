package org.example.audio_ecommerce.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutOverviewResponse {

    private PayoutGroup undeliCOD;       // chưa giao COD
    private PayoutGroup undeliONLINE;    // chưa giao online

    private PayoutGroup deliCOD;         // đã giao COD
    private PayoutGroup deliONLINE;      // đã giao online

    private PayoutGroup platformFee;     // tổng phí nền tảng cần thu
    private PayoutGroup totalPaid;       // tổng đã trả cho shop
}
