package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public void setBuyable(UUID customerId, boolean buyable) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
        c.setBuyable(buyable);
        customerRepository.save(c);
    }
}

