package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyStatsResponse {

    // meta filter
    private String currency;         // default "VND"
    private LocalDateTime from;
    private LocalDateTime to;

    // Thống kê số dư hiện tại (snapshot)
    private PlatformMoney platform;
    private StoreMoney store;
    private CustomerMoney customer;

    // Tổng tiền theo bộ lọc "type" (sum amount theo transaction type)
    // Ví dụ: type=RELEASE_PENDING => sum theo store_wallet_transactions.type
    private List<TypeSummary> typeSummaries;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlatformMoney {
        private BigDecimal totalBalance;
        private BigDecimal pendingBalance;
        private BigDecimal doneBalance;
        private BigDecimal receivedTotal;
        private BigDecimal refundedTotal;
        private BigDecimal commissionBalance;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StoreMoney {
        // Nếu truyền storeId thì là 1 store; nếu null thì là tổng tất cả store
        private UUID storeId;

        private BigDecimal availableBalance;
        private BigDecimal pendingBalance;
        private BigDecimal depositBalance;
        private BigDecimal totalRevenue;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerMoney {
        // Nếu truyền customerId thì là 1 customer; nếu null thì là tổng tất cả customer
        private UUID customerId;

        private BigDecimal balance;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TypeSummary {
        private String walletSystem;          // PLATFORM / STORE / CUSTOMER
        private String type;                  // enum name
        private BigDecimal totalAmount;       // SUM(amount) theo filter
    }
}
