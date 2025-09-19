package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.LoginResponse;
import org.example.audio_ecommerce.dto.response.RegisterResponse;
import org.example.audio_ecommerce.dto.response.AccountResponse;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Role;
import org.example.audio_ecommerce.entity.RoleEnum;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.RoleRepository;
import org.example.audio_ecommerce.security.JwtTokenProvider;
import org.example.audio_ecommerce.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public ResponseEntity<BaseResponse> create(RegisterRequest request) {
        if (repository.existsByEmailIgnoreCase(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Email already in use", null));
        }

        // Lấy role mặc định cho người dùng mới (khuyến nghị ROLE_USER)
        // Nếu RoleRepository của bạn trả Optional<Role>, đổi theo .orElseThrow(...)
        Role role = roleRepository.findByName(RoleEnum.CUSTOMER);

        Account entity = Account.builder()
                .name(request.getName())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        repository.save(entity);

        RegisterResponse response = new RegisterResponse();
        response.setEmail(entity.getEmail());
        response.setName(entity.getName());
        // (tuỳ) có thể add role vào response nếu cần:
        // response.setRole(role.getName().name());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(201, "Created", response));
    }

    @Override
    public ResponseEntity<BaseResponse> login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()
                    )
            );

            // username ở đây chính là email
            String username = authentication.getName();

            // Sinh token theo JwtTokenProvider mới
            String accessToken = jwtTokenProvider.generateAccessToken(username);
            String refreshToken = jwtTokenProvider.generateRefreshToken(username);

            var account = repository.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            var user = new AccountResponse(
                    account.getEmail(),
                    account.getName(),
                    account.getRole().getName().name()
            );

            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, user);

            return ResponseEntity.ok(new BaseResponse<>(200, "Success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }
}
