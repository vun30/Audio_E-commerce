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

import java.util.List;

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
        // ✅ Expect: username = email:ROLE (đồng bộ với token)
        String[] parts = usernameWithRole.split(":");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Username must be in format 'email:ROLE'");
        }

        String email = parts[0];
        String roleName = parts[1];

        Account acc = repo.findByEmailAndRole(email, RoleEnum.valueOf(roleName))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email " + email + " and role " + roleName));

        var authority = new SimpleGrantedAuthority("ROLE_" + acc.getRole().name());

        return org.springframework.security.core.userdetails.User
                .withUsername(email + ":" + acc.getRole().name()) // đồng bộ
                .password(acc.getPassword())
                .authorities(List.of(authority))
                .disabled(false)
                .build();
    }
}
