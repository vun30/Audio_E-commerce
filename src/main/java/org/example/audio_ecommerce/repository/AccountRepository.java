package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailAndRole(String email, RoleEnum role);
    boolean existsByEmailAndRole(String email, RoleEnum role);
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByPhone(String phoneNumber);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByResetPasswordToken(String token);


}