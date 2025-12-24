package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "role"}) // Email + Role phải duy nhất
    }
)
public class Account extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoleEnum role;

    @Column(length = 255)
    private String resetPasswordToken;

    // Account.java
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Column(name = "email_verify_token_expiry")
    private java.time.LocalDateTime emailVerifyTokenExpiry;

    @Column
    private java.time.LocalDateTime resetPasswordTokenExpiry;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Store store;
}
