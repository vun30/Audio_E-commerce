package org.example.audio_ecommerce.repository.projection;

import org.example.audio_ecommerce.entity.Enum.PlatformRevenueType;

import java.math.BigDecimal;

public interface PlatformRevenueAgg {
    PlatformRevenueType getType();
    BigDecimal getTotalAmount();
}
