package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse create(CustomerCreateRequest req);
    CustomerResponse get(UUID id);
    Page<CustomerResponse> search(String keyword, CustomerStatus status, Pageable pageable);
    CustomerResponse update(UUID id, CustomerUpdateRequest req);
    void softDelete(UUID id);

    // Address
    List<AddressResponse> listAddresses(UUID customerId);
    AddressResponse addAddress(UUID customerId, AddressCreateRequest req);
    AddressResponse updateAddress(UUID customerId, UUID addressId, AddressUpdateRequest req);
    void deleteAddress(UUID customerId, UUID addressId);
    void setDefaultAddress(UUID customerId, UUID addressId);
}
