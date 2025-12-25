package org.example.audio_ecommerce.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformUserStoreGrowthPoint {
    private int year;
    private int month;          // 1..12
    private long newCustomers;  // số customer mới trong tháng
    private long newStores;     // số store mới trong tháng
}
