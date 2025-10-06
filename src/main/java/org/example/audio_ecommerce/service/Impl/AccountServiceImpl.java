package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Customer;                      // ✅
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;         // ✅
import org.example.audio_ecommerce.entity.Enum.KycStatus;              // ✅
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.entity.Enum.StoreStatus;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.CustomerRepository;       // ✅
import org.example.audio_ecommerce.repository.StoreRepository;
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
import org.springframework.transaction.annotation.Transactional;            // ✅

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;               // ✅
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

    @Transactional  // ✅ tạo Account + Customer trong 1 giao dịch
    public ResponseEntity<BaseResponse> register(RegisterRequest request, RoleEnum role, String successMsg) {
        if (repository.existsByEmailAndRole(request.getEmail(), role)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Email already used with role " + role, null));
        }
        if (repository.existsByPhone(request.getPhone())){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Phone number already used", null));
        }
        // ✅ Tạo tài khoản
        Account entity = Account.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        repository.save(entity);

        // ✅ Tạo Customer mặc định cho account (mọi role)
        createDefaultCustomerForAccount(entity);

        // ✅ Nếu role là STOREOWNER → tạo store mặc định
        if (role == RoleEnum.STOREOWNER) {
            createDefaultStoreForAccount(entity);
        }

        RegisterResponse response = new RegisterResponse(entity.getEmail(), entity.getName(), entity.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(201, successMsg, response));
    }

    // ==================== Helpers ====================

    /** ✅ Luôn tạo hồ sơ Customer 1–1 cho Account */
    private void createDefaultCustomerForAccount(Account account) {
        if (customerRepository.existsByAccount_Id(account.getId())) return;

        // username mặc định = phần trước @ của email
        String defaultUsername = account.getEmail().split("@")[0];

        Customer customer = Customer.builder()
                .account(account)                          // 🔗 liên kết 1–1
                .fullName(account.getName())
                .userName(defaultUsername)
                .email(account.getEmail())
                .phoneNumber(account.getPhone())                         // có thể cập nhật sau qua API profile
                .passwordHash(account.getPassword())       // dùng hash đã encode ở Account
                .status(CustomerStatus.ACTIVE)
                .twoFactorEnabled(false)
                .kycStatus(KycStatus.NONE)
                .build();

        customerRepository.save(customer);
    }

    /** ✅ Tạo store mặc định khi account có role STOREOWNER */
    private void createDefaultStoreForAccount(Account account) {
        if (storeRepository.existsByAccount_Id(account.getId())) return;

        Store store = Store.builder()
                .account(account)
                .walletId(UUID.randomUUID())
                .storeName("Store of " + account.getName())
                .description("This store is created automatically and is inactive until KYC is approved.")
                .status(StoreStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        storeRepository.save(store);
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
            String usernameWithRole = request.getEmail() + ":" + role.name();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameWithRole, request.getPassword())
            );

            Account user = repository.findByEmailAndRole(request.getEmail(), role)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with this role"));

            String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
            AccountResponse userResponse = new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());
            LoginResponse loginResponse = new LoginResponse(token, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }
}
