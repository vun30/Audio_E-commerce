package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.service.LegalPointService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerLegalPointResetCron {

    private final CustomerRepository customerRepository;
    private final LegalPointService legalPointService;
    // chạy mỗi ngày 02:00
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void resetLegalPointIfZeroFor30Days() {
        List<Customer> locked =
                customerRepository.findAllByBuyableFalseAndLegalPointZeroedAtIsNotNullAndLegalPoint(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        for (Customer c : locked) {
            LocalDateTime zeroAt = c.getLegalPointZeroedAt();
            if (zeroAt == null) continue;

            // ✅ đủ 30 ngày và vẫn đang = 0 -> reset
            if (!zeroAt.isAfter(now.minusDays(30))) {
                c.setLegalPoint(new BigDecimal("10"));
                c.setBuyable(true);
                c.setLegalPointZeroedAt(null);
                log.info("Reset legalPoint to 10 for customerId={}", c.getId());
            }
        }
        legalPointService.resetCustomerLegalPointAfter30Days();
        customerRepository.saveAll(locked);
    }
}
