package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.PlatformFee;
import java.util.List;
import java.util.UUID;

public interface PlatformFeeService {
    PlatformFee create(PlatformFee fee);
    PlatformFee update(UUID id, PlatformFee fee);
    PlatformFee getActiveFee();
    List<PlatformFee> getAll();
}
