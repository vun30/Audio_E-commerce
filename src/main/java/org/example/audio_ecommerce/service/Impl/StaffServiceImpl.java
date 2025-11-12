package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StaffCreateRequest;
import org.example.audio_ecommerce.dto.response.StaffResponse;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.entity.Staff;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.StaffRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StaffService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {
    private final StaffRepository staffRepository;
    private final StoreRepository storeRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(UUID storeId, StaffCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        // Kiểm tra trùng email tối ưu
        if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại!");
        }
        // Tạo mới Account cho staff
        Account account = new Account();
        account.setName(request.getFullName());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setPhone(request.getPhone());
        account.setRole(RoleEnum.STAFF);
        account = accountRepository.save(account);
        // Tạo Staff và gán account
        Staff staff = new Staff();
        staff.setUsername(request.getUsername());
        staff.setPassword(passwordEncoder.encode(request.getPassword())); // Có thể bỏ nếu chỉ dùng account
        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setStore(store);
        staff.setAccount(account);
        staff = staffRepository.save(staff);
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setUsername(staff.getUsername());
        response.setFullName(staff.getFullName());
        response.setEmail(staff.getEmail());
        response.setPhone(staff.getPhone());
        response.setStoreId(store.getStoreId().toString());
        return response;
    }

    @Override
    public List<StaffResponse> getAllStaffByStoreId(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        return staffRepository.findByStore(store).stream()
                .map(this::mapToStaffResponse)
                .toList();
    }

    @Override
    public StaffResponse getStaffById(UUID storeId, UUID staffId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Staff staff = staffRepository.findByIdAndStore(staffId, store)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found in this store"));

        return mapToStaffResponse(staff);
    }

    // Helper method để tái sử dụng
    private StaffResponse mapToStaffResponse(Staff staff) {
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setUsername(staff.getUsername());
        response.setFullName(staff.getFullName());
        response.setEmail(staff.getEmail());
        response.setPhone(staff.getPhone());
        response.setStoreId(staff.getStore().getStoreId().toString());
        return response;
    }
}
