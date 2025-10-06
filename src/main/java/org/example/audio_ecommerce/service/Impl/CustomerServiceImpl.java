package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.CustomerAddressRepository;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepo;
    private final CustomerAddressRepository addressRepo;
    private final PasswordEncoder passwordEncoder; // cần bean BCrypt

    // ===== Customers =====

    @Override
    public CustomerResponse create(CustomerCreateRequest req) {
        if (customerRepo.existsByEmailIgnoreCase(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại");
        if (customerRepo.existsByPhoneNumber(req.getPhoneNumber()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại");

        Customer c = Customer.builder()
                .fullName(req.getFullName())
                .userName(req.getUserName())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .avatarURL(req.getAvatarURL())
                .twoFactorEnabled(Boolean.TRUE.equals(req.getTwoFactorEnabled()))
                .status(CustomerStatus.ACTIVE)
                .kycStatus(KycStatus.NONE)
                .preferredCategory(req.getPreferredCategory())
                .build();

        customerRepo.save(c);
        return toCustomerResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        return toCustomerResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String keyword, CustomerStatus status, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return customerRepo.search(kw, status, pageable).map(this::toCustomerResponse);
    }

    @Override
    public CustomerResponse update(UUID id, CustomerUpdateRequest req) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(c.getEmail())
                && customerRepo.existsByEmailIgnoreCaseAndIdNot(req.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại");
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().equals(c.getPhoneNumber())
                && customerRepo.existsByPhoneNumberAndIdNot(req.getPhoneNumber(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SĐT đã tồn tại");
        }

        if (req.getFullName() != null) c.setFullName(req.getFullName());
        if (req.getUserName() != null) c.setUserName(req.getUserName());
        if (req.getEmail() != null) c.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null) c.setPhoneNumber(req.getPhoneNumber());
        if (req.getGender() != null) c.setGender(req.getGender());
        if (req.getDateOfBirth() != null) c.setDateOfBirth(req.getDateOfBirth());
        if (req.getAvatarURL() != null) c.setAvatarURL(req.getAvatarURL());
        if (req.getStatus() != null) c.setStatus(req.getStatus());
        if (req.getTwoFactorEnabled() != null) c.setTwoFactorEnabled(req.getTwoFactorEnabled());
        if (req.getKycStatus() != null) c.setKycStatus(req.getKycStatus());
        if (req.getPreferredCategory() != null) c.setPreferredCategory(req.getPreferredCategory());
        if (req.getLoyaltyPoints() != null) c.setLoyaltyPoints(req.getLoyaltyPoints());
        if (req.getLoyaltyLevel() != null) c.setLoyaltyLevel(req.getLoyaltyLevel());

        return toCustomerResponse(c);
    }

    @Override
    public void softDelete(UUID id) {
        Customer c = customerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        c.setStatus(CustomerStatus.DELETED);
    }

    // ===== Addresses =====

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID customerId) {
        ensureCustomerExists(customerId);
        return addressRepo.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId)
                .stream().map(this::toAddressResponse).collect(Collectors.toList());
    }

    @Override
    public AddressResponse addAddress(UUID customerId, AddressCreateRequest req) {
        Customer c = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        CustomerAddress a = CustomerAddress.builder()
                .customer(c)
                .receiverName(req.getReceiverName())
                .phoneNumber(req.getPhoneNumber())
                .label(req.getLabel())
                .country(req.getCountry())
                .province(req.getProvince())
                .district(req.getDistrict())
                .ward(req.getWard())
                .street(req.getStreet())
                .addressLine(req.getAddressLine())
                .postalCode(req.getPostalCode())
                .note(req.getNote())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        addressRepo.save(a);

        // đồng bộ default
        if (Boolean.TRUE.equals(req.getIsDefault()) || c.getDefaultAddress() == null) {
            setDefaultInternal(c, a);
        } else {
            c.setAddressCount((int) addressRepo.countByCustomerId(customerId));
        }
        return toAddressResponse(a);
    }

    @Override
    public AddressResponse updateAddress(UUID customerId, UUID addressId, AddressUpdateRequest req) {
        Customer c = ensureCustomerExists(customerId);
        CustomerAddress a = addressRepo.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        if (req.getReceiverName() != null) a.setReceiverName(req.getReceiverName());
        if (req.getPhoneNumber() != null) a.setPhoneNumber(req.getPhoneNumber());
        if (req.getLabel() != null) a.setLabel(req.getLabel());
        if (req.getCountry() != null) a.setCountry(req.getCountry());
        if (req.getProvince() != null) a.setProvince(req.getProvince());
        if (req.getDistrict() != null) a.setDistrict(req.getDistrict());
        if (req.getWard() != null) a.setWard(req.getWard());
        if (req.getStreet() != null) a.setStreet(req.getStreet());
        if (req.getAddressLine() != null) a.setAddressLine(req.getAddressLine());
        if (req.getPostalCode() != null) a.setPostalCode(req.getPostalCode());
        if (req.getNote() != null) a.setNote(req.getNote());

        if (req.getIsDefault() != null && req.getIsDefault()) {
            setDefaultInternal(c, a);
        }
        return toAddressResponse(a);
    }

    @Override
    public void deleteAddress(UUID customerId, UUID addressId) {
        Customer c = ensureCustomerExists(customerId);
        CustomerAddress a = addressRepo.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        boolean wasDefault = a.isDefault();
        addressRepo.delete(a);

        c.setAddressCount((int) addressRepo.countByCustomerId(customerId));

        if (wasDefault) {
            addressRepo.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId).stream()
                    .findFirst().ifPresent(addr -> setDefaultInternal(c, addr));
        }
    }

    @Override
    public void setDefaultAddress(UUID customerId, UUID addressId) {
        Customer c = ensureCustomerExists(customerId);
        CustomerAddress a = addressRepo.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));
        setDefaultInternal(c, a);
    }

    // ===== Helpers =====

    private Customer ensureCustomerExists(UUID id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    private void setDefaultInternal(Customer c, CustomerAddress target) {
        // reset cờ isDefault cho tất cả địa chỉ của customer
        addressRepo.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(c.getId()).forEach(addr ->
                addr.setDefault(addr.getId().equals(target.getId())));
        c.setDefaultAddress(target);
        c.setAddressCount((int) addressRepo.countByCustomerId(c.getId()));
    }

    private CustomerResponse toCustomerResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .userName(c.getUserName())
                .email(c.getEmail())
                .phoneNumber(c.getPhoneNumber())
                .gender(c.getGender())
                .dateOfBirth(c.getDateOfBirth())
                .avatarURL(c.getAvatarURL())
                .status(c.getStatus())
                .twoFactorEnabled(c.isTwoFactorEnabled())
                .kycStatus(c.getKycStatus())
                .lastLogin(c.getLastLogin())
                .addressCount(c.getAddressCount())
                .loyaltyPoints(c.getLoyaltyPoints())
                .loyaltyLevel(c.getLoyaltyLevel())
                .voucherCount(c.getVoucherCount())
                .orderCount(c.getOrderCount())
                .cancelCount(c.getCancelCount())
                .returnCount(c.getReturnCount())
                .unpaidOrderCount(c.getUnpaidOrderCount())
                .lastOrderDate(c.getLastOrderDate())
                .preferredCategory(c.getPreferredCategory())
                .build();
    }

    private AddressResponse toAddressResponse(CustomerAddress a) {
        return AddressResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer().getId())
                .receiverName(a.getReceiverName())
                .phoneNumber(a.getPhoneNumber())
                .label(a.getLabel())
                .country(a.getCountry())
                .province(a.getProvince())
                .district(a.getDistrict())
                .ward(a.getWard())
                .street(a.getStreet())
                .addressLine(a.getAddressLine())
                .postalCode(a.getPostalCode())
                .note(a.getNote())
                .isDefault(a.isDefault())
                .build();
    }
}
