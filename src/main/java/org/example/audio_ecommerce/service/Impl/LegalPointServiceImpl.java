package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.LegalPointService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalPointServiceImpl implements LegalPointService {

    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public void minusForStore(UUID storeId, int point) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        // legalPoint là BigDecimal
        BigDecimal current = store.getLegalPoint();
        BigDecimal safeCurrent = (current == null)
                ? BigDecimal.ZERO
                : current;

        // trừ điểm (1 điểm = 1 đơn vị)
        store.setLegalPoint(
                safeCurrent.subtract(BigDecimal.valueOf(point))
        );

        storeRepository.save(store);
    }
}
