// package org.example.audio_ecommerce.dto.response;
package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StoreWalletSummaryFinalResponse {

    private UUID storeId;

    // Ước tính doanh thu từ các item chưa payout
    private BigDecimal estimatedGross;

    // Đang bị hold (chưa đủ điều kiện payout)
    private BigDecimal pendingGross;

    // Doanh thu đã payout (gross trước phí & ship chênh lệch)
    private BigDecimal doneGross;

    // Lãi ròng sau khi trừ phí nền tảng, ship chênh lệch, giá vốn
    private BigDecimal netProfit;


}
