package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.GhnFeeRequest;

public interface GhnFeeService {
    String calculateFeeRaw(GhnFeeRequest request); // trả về JSON gốc GHN
}
