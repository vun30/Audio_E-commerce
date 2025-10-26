package org.example.audio_ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class CodConfig {
    // 1.0 = yêu cầu đủ 100% giá trị đơn hàng store (mặc định an toàn và có thể chỉnh sửa)
    @Value("${cod.deposit.ratio}")
    private BigDecimal codDepositRatio;

    public BigDecimal getCodDepositRatio() {
        return codDepositRatio;
    }
}
