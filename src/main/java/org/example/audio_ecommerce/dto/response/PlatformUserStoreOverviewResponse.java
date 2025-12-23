package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformUserStoreOverviewResponse {

    // tổng all
    private long totalCustomerAccounts;  // tổng account role CUSTOMER
    private long totalStores;            // tổng shop

    // theo tháng đang chọn (hoặc tháng hiện tại nếu không truyền)
    private int year;
    private int month;

    private long newCustomersInMonth;    // số customer mới trong tháng
    private long newStoresInMonth;       // số store mới trong tháng

    // tháng trước
    private long newCustomersPrevMonth;
    private long newStoresPrevMonth;

    // tăng trưởng % so với tháng trước
    private BigDecimal customerGrowthPercent; // ví dụ: 25.50 (%)
    private BigDecimal storeGrowthPercent;
}
