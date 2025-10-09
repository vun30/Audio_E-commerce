package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.repository.CustomerRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DomainProvisioningService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final WalletRepository walletRepository;

    /** Tạo Customer + Wallet mặc định nếu chưa có */
    @Transactional
    public void ensureCustomerAndWallet(Account account) {
        if (!customerRepository.existsByAccount_Id(account.getId())) {
            String defaultUsername = account.getEmail().split("@")[0];
            Customer c = Customer.builder()
                    .account(account)
                    .fullName(account.getName())
                    .userName(defaultUsername)
                    .email(account.getEmail())
                    .phoneNumber(account.getPhone())
                    .passwordHash(account.getPassword())
                    .status(CustomerStatus.ACTIVE)
                    .twoFactorEnabled(false)
                    .kycStatus(KycStatus.NONE)
                    .build();
            customerRepository.save(c);

            if (!walletRepository.existsByCustomer_Id(c.getId())) {
                Wallet w = Wallet.builder().customer(c).build();
                walletRepository.save(w);
            }
        }
    }

    /** (Tuỳ chọn) tạo Store mặc định cho STOREOWNER */
    @Transactional
    public void ensureDefaultStore(Account account) {
        if (account.getRole() != RoleEnum.STOREOWNER) return;
        if (!storeRepository.existsByAccount_Id(account.getId())) {
            Store s = Store.builder()
                    .account(account)
                    .walletId(UUID.randomUUID())
                    .storeName("Store of " + account.getName())
                    .description("This store is created automatically and is inactive until KYC is approved.")
                    .status(StoreStatus.INACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            storeRepository.save(s);
        }
    }
}
