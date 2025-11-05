package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.StaffCreateRequest;
import org.example.audio_ecommerce.dto.response.StaffResponse;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    StaffResponse createStaff(UUID storeId, StaffCreateRequest request);
    List<StaffResponse> getAllStaffByStoreId(UUID storeId);
    StaffResponse getStaffById(UUID storeId, UUID staffId);
}

