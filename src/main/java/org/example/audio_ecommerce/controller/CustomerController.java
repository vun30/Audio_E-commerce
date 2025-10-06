package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.example.audio_ecommerce.service.CustomerService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ===== Customers =====

//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public CustomerResponse create(@Valid @RequestBody CustomerCreateRequest req) {
//        return customerService.create(req);
//    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return customerService.get(id);
    }

    @GetMapping
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = sort.endsWith(",asc")
                ? Sort.by(sort.split(",")[0]).ascending()
                : Sort.by(sort.split(",")[0]).descending();
        Pageable pageable = PageRequest.of(page, size, s);
        return customerService.search(keyword, status, pageable);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody CustomerUpdateRequest req) {
        return customerService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable UUID id) {
        customerService.softDelete(id);
    }

    // ===== Addresses (nested) =====

    @GetMapping("/{customerId}/addresses")
    public List<AddressResponse> listAddresses(@PathVariable UUID customerId) {
        return customerService.listAddresses(customerId);
    }

    @PostMapping("/{customerId}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@PathVariable UUID customerId,
                                      @Valid @RequestBody AddressCreateRequest req) {
        return customerService.addAddress(customerId, req);
    }

    @PutMapping("/{customerId}/addresses/{addressId}")
    public AddressResponse updateAddress(@PathVariable UUID customerId,
                                         @PathVariable UUID addressId,
                                         @Valid @RequestBody AddressUpdateRequest req) {
        return customerService.updateAddress(customerId, addressId, req);
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable UUID customerId,
                              @PathVariable UUID addressId) {
        customerService.deleteAddress(customerId, addressId);
    }

    @PostMapping("/{customerId}/addresses/{addressId}/set-default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setDefault(@PathVariable UUID customerId,
                           @PathVariable UUID addressId) {
        customerService.setDefaultAddress(customerId, addressId);
    }
}
