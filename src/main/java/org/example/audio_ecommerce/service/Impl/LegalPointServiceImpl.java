package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.LegalPointService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalPointServiceImpl implements LegalPointService {

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void minusForStore(UUID storeId, int point) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        BigDecimal current = store.getLegalPoint() == null ? BigDecimal.ZERO : store.getLegalPoint();
        BigDecimal next = current.subtract(BigDecimal.valueOf(point));
        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;

        store.setLegalPoint(next);
        storeRepository.save(store);
    }

    // ✅ NEW: trừ điểm customer + khóa mua nếu về 0
    @Override
    @Transactional
    public void minusForCustomer(UUID customerId, int point, String reason) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        BigDecimal current = c.getLegalPoint() == null ? BigDecimal.ZERO : c.getLegalPoint();
        BigDecimal next = current.subtract(BigDecimal.valueOf(point));
        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;

        c.setLegalPoint(next);

        // ✅ nếu về 0 -> khóa mua + set mốc (chỉ set lần đầu)
        if (next.compareTo(BigDecimal.ZERO) == 0) {
            c.setBuyable(false);
            if (c.getLegalPointZeroedAt() == null) {
                c.setLegalPointZeroedAt(LocalDateTime.now());
            }
        }

        // nếu vẫn > 0 thì không đụng buyable/zeroedAt
        customerRepository.save(c);

        // TODO (tuỳ bạn): lưu log reason vào bảng history
    }

    // ✅ NEW: reset sau 30 ngày, chỉ khi điểm vẫn = 0
    @Override
    @Transactional
    public void resetCustomerLegalPointAfter30Days() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);

        List<Customer> lockedZero = customerRepository.findAllByBuyableFalseAndLegalPointZeroedAtIsNotNullAndLegalPoint(BigDecimal.ZERO);

        for (Customer c : lockedZero) {
            if (c.getLegalPointZeroedAt() != null && !c.getLegalPointZeroedAt().isAfter(deadline)) {
                // ✅ chỉ reset nếu hiện tại vẫn = 0
                if (c.getLegalPoint() == null || c.getLegalPoint().compareTo(BigDecimal.ZERO) == 0) {
                    c.setLegalPoint(new BigDecimal("10"));
                    c.setBuyable(true);
                    c.setLegalPointZeroedAt(null);
                }
            }
        }

        customerRepository.saveAll(lockedZero);
    }
}
