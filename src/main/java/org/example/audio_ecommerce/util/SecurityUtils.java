package org.example.audio_ecommerce.util;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final StoreRepository storeRepo;


    /** 🔹 Lấy username dạng email:ROLE từ SecurityContext */
    private String getRawUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getName();
    }

    /** 🔹 Tách email + role */
    private String[] extractEmailAndRole() {
        String raw = getRawUsername();
        if (raw == null) throw new RuntimeException("User not authenticated");

        String[] parts = raw.split(":");
        if (parts.length != 2) throw new RuntimeException("Invalid username format");

        return parts; // [email, ROLE]
    }

    /** 🔹 Load Account từ SecurityContext */
    public Account getCurrentAccount() {
        String[] parts = extractEmailAndRole();
        String email = parts[0];
        RoleEnum role = RoleEnum.valueOf(parts[1]);

        return accountRepo.findByEmailAndRole(email, role)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    /** 🔹 Lấy customerId */
    public UUID getCurrentCustomerId() {
        Account acc = getCurrentAccount();
        Customer customer = customerRepo.findByAccount(acc)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return customer.getId();
    }

    /** 🔹 Lấy storeId */
    public UUID getCurrentStoreId() {
        Account acc = getCurrentAccount();
        Store store = storeRepo.findByAccount(acc)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        return store.getStoreId();
    }


}
