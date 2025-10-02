package org.example.audio_ecommerce.service.Impl;

import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Profile("!test")
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository repo;

    public CustomUserDetailsService(AccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameWithRole) throws UsernameNotFoundException {
        // ✅ Expect format: email:ROLE
        String[] parts = usernameWithRole.split(":");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Username must be in format 'email:ROLE'");
        }

        String email = parts[0];
        RoleEnum role;
        try {
            role = RoleEnum.valueOf(parts[1]); // convert string -> enum
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("Invalid role in username");
        }

        // ✅ Tìm account theo email + role
        Account acc = repo.findByEmailAndRole(email, role)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with role " + role));

        var authority = new SimpleGrantedAuthority("ROLE_" + acc.getRole().name());

        return org.springframework.security.core.userdetails.User.withUsername(email + ":" + acc.getRole().name())
                .password(acc.getPassword())
                .authorities(authority)
                .disabled(false) // bạn có thể dùng acc.isActive() nếu có field active
                .build();
    }
}
