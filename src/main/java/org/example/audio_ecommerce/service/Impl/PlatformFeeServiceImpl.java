package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.PlatformFee;
import org.example.audio_ecommerce.repository.PlatformFeeRepository;
import org.example.audio_ecommerce.service.PlatformFeeService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformFeeServiceImpl implements PlatformFeeService {

    private final PlatformFeeRepository repository;

    @Override
    public PlatformFee create(PlatformFee fee) {
        // Hủy kích hoạt fee hiện tại
        repository.findByIsActiveTrue().ifPresent(f -> {
            f.setIsActive(false);
            repository.save(f);
        });

        fee.setCreatedAt(LocalDateTime.now());
        fee.setEffectiveDate(LocalDateTime.now());
        fee.setIsActive(true);
        return repository.save(fee);
    }

    @Override
    public PlatformFee update(UUID id, PlatformFee fee) {
        PlatformFee existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phí sàn"));
        existing.setPercentage(fee.getPercentage());
        existing.setDescription(fee.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public PlatformFee getActiveFee() {
        return repository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("Chưa có phí sàn nào hoạt động"));
    }

    @Override
    public List<PlatformFee> getAll() {
        return repository.findAll();
    }
}
