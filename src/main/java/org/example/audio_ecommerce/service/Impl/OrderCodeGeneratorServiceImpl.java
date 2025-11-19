package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.OrderCodeSequence;
import org.example.audio_ecommerce.repository.OrderCodeSequenceRepository;
import org.example.audio_ecommerce.service.OrderCodeGeneratorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderCodeGeneratorServiceImpl implements OrderCodeGeneratorService {

    private final OrderCodeSequenceRepository seqRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyy");

    @Override
    @Transactional
    public String nextOrderCode() {
        LocalDate today = LocalDate.now();
        OrderCodeSequence seq = seqRepo.findByOrderDate(today)
                .orElseGet(() -> {
                    OrderCodeSequence s = new OrderCodeSequence();
                    s.setOrderDate(today);
                    s.setLastNumber(0);
                    return s;
                });

        int next = seq.getLastNumber() + 1;
        seq.setLastNumber(next);
        seqRepo.save(seq);

        String datePart = today.format(DATE_FMT);            // 191125
        String numberPart = String.format("%06d", next);     // 000001
        return "DATS" + datePart + numberPart;                 // HĐ191125000001
    }
}
