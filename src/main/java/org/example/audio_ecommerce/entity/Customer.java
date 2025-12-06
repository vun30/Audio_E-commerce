package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.example.audio_ecommerce.entity.Enum.Gender;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.Enum.LoyaltyLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_customer_phone", columnNames = "phone_number")
        },
        indexes = {
                @Index(name = "idx_customer_username", columnList = "username"),
                @Index(name = "idx_customer_status", columnList = "status"),
                @Index(name = "idx_customer_kyc", columnList = "kyc_status")
        }
)
public class Customer extends BaseEntity {

    // ===== Info =====
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "username", length = 100, nullable = false)
    private String userName;

    @Email
    @NotBlank
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone_number", length = 30, nullable = true)
    private String phoneNumber;

    @NotBlank
    @Size(min = 6)
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", columnDefinition = "LONGTEXT")
    private String avatarURL;


    // ===== Status / Security / KYC =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private boolean twoFactorEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20, nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NONE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ===== Addresses =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_address_id", columnDefinition = "CHAR(36)")
    private CustomerAddress defaultAddress;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CustomerAddress> addresses = new ArrayList<>();

    @Column(name = "address_count", nullable = false)
    @Builder.Default
    private int addressCount = 0;

    // ===== Loyalty / Vouchers =====
    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private int loyaltyPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_level", length = 10)
    private LoyaltyLevel loyaltyLevel;

    @Column(name = "voucher_count", nullable = false)
    @Builder.Default
    private int voucherCount = 0;

    // ===== Order Counters =====
    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private int orderCount = 0;

    @Column(name = "cancel_count", nullable = false)
    @Builder.Default
    private int cancelCount = 0;

    @Column(name = "return_count", nullable = false)
    @Builder.Default
    private int returnCount = 0;

    @Column(name = "unpaid_order_count", nullable = false)
    @Builder.Default
    private int unpaidOrderCount = 0;

    @Column(name = "last_order_date")
    private LocalDate lastOrderDate;

    @Column(name = "preferred_category", length = 255)
    private String preferredCategory;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private Account account;

    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Wallet wallet;

    // ===== Helpers =====
    public void addAddress(CustomerAddress addr, boolean makeDefault) {
        addr.setCustomer(this);
        addresses.add(addr);
        addressCount = addresses.size();
        if (makeDefault || defaultAddress == null) {
            setDefaultAddress(addr);
        }
    }

    public void setDefaultAddress(CustomerAddress addr) {
        this.defaultAddress = addr;
        // đồng bộ cờ isDefault trong list
        addresses.forEach(a -> a.setDefault(a == addr || a.getId() != null && a.getId().equals(addr.getId())));
    }

    public void removeAddress(CustomerAddress addr) {
        addresses.remove(addr);
        addr.setCustomer(null);
        addressCount = addresses.size();
        if (defaultAddress != null && defaultAddress.equals(addr)) {
            defaultAddress = addresses.stream().findFirst().orElse(null);
            if (defaultAddress != null) setDefaultAddress(defaultAddress);
        }
    }

    public void increaseOrderCounters(boolean isPaid) {
        this.orderCount++;
        if (!isPaid) this.unpaidOrderCount++;
        this.lastOrderDate = LocalDate.now();
    }
}
