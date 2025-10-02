package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.repository.AccountRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ==================== REGISTER ====================
    @Override
    public ResponseEntity<BaseResponse> registerCustomer(RegisterRequest request) {
        return register(request, RoleEnum.CUSTOMER, "Customer created");
    }

    @Override
    public ResponseEntity<BaseResponse> registerStore(RegisterRequest request) {
        return register(request, RoleEnum.STOREOWNER, "Store Owner created");
    }

    @Override
    public ResponseEntity<BaseResponse> registerAdmin(RegisterRequest request) {
        return register(request, RoleEnum.ADMIN, "Admin created");
    }

    private ResponseEntity<BaseResponse> register(RegisterRequest request, RoleEnum role, String successMsg) {
        if (repository.existsByEmailAndRole(request.getEmail(), role)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Email already used with role " + role, null));
        }

        Account entity = Account.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        repository.save(entity);

        RegisterResponse response = new RegisterResponse(entity.getEmail(), entity.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(201, successMsg, response));
    }

    // ==================== LOGIN ====================
    @Override
    public ResponseEntity<BaseResponse> loginCustomer(LoginRequest request) {
        return login(request, RoleEnum.CUSTOMER);
    }

    @Override
    public ResponseEntity<BaseResponse> loginStore(LoginRequest request) {
        return login(request, RoleEnum.STOREOWNER);
    }

    @Override
    public ResponseEntity<BaseResponse> loginAdmin(LoginRequest request) {
        return login(request, RoleEnum.ADMIN);
    }

    private ResponseEntity<BaseResponse> login(LoginRequest request, RoleEnum role) {
        try {
            // ✅ AUTH step: sử dụng email:ROLE làm username
            String usernameWithRole = request.getEmail() + ":" + role.name();
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameWithRole, request.getPassword())
            );

            // ✅ DB check email + role
            Account user = repository.findByEmailAndRole(request.getEmail(), role)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with this role"));

            // ✅ TOKEN: subject = email:ROLE (đồng bộ với CustomUserDetailsService)
            String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            LoginResponse loginResponse = new LoginResponse(token, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }
}
