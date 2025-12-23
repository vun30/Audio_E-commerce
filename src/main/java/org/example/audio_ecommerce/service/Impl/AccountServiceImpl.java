package org.example.audio_ecommerce.service.Impl;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.email.AccountData;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.email.dto.AccountVerifyData;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.security.JwtTokenProvider;
import org.example.audio_ecommerce.service.AccountService;
import org.example.audio_ecommerce.service.FirebaseAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final WalletRepository walletRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;
    private final StaffRepository staffRepository;
    private final FirebaseAuthService firebaseAuthService;

    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173/verrify-register-account}")
    private String frontendUrl;

    // =====================================================
    // REGISTER
    // =====================================================
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

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> registerFlatStaff(RegisterRequest request) {
        return register(request, RoleEnum.FLATSTAFF, "Flat Staff created");
    }

    @Transactional
    public ResponseEntity<BaseResponse> register(RegisterRequest request, RoleEnum role, String successMsg) {
        if (repository.existsByEmailAndRole(request.getEmail(), role)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Email already used with role " + role, null));
        }
        if (repository.existsByPhone(request.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BaseResponse<>(409, "Phone number already used", null));
        }

        // ✅ 1) Tạo Account
        Account entity = Account.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .emailVerified(false)
                .build();

        // ✅ NEW: Customer + Store đăng ký xong -> inactive cho tới khi verify
        if (role == RoleEnum.CUSTOMER || role == RoleEnum.STOREOWNER) {
            entity.setActive(false);
        } else {
            entity.setActive(true);
        }

        repository.save(entity);

        // ✅ 2) Issue token verify
        issueEmailVerifyToken(entity);

        // ✅ 3) Tạo Customer mặc định
        createDefaultCustomerForAccount(entity);

        // ✅ 4) Nếu STOREOWNER → tạo Store + Wallet
        if (role == RoleEnum.STOREOWNER) {
            createDefaultStoreWithWallet(entity);
        }

        // ✅ 5) Gửi email có link verify (không để fail đăng ký)
        sendVerifyEmailSafe(entity);

        RegisterResponse response =
                new RegisterResponse(entity.getEmail(), entity.getName(), entity.getPhone());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(201, successMsg, response));
    }

    // =====================================================
    // LOGIN
    // =====================================================
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

    @Override
    public ResponseEntity<BaseResponse> loginFlatStaff(LoginRequest request) {
        return login(request, RoleEnum.FLATSTAFF);
    }

    @Override
    public ResponseEntity<BaseResponse> loginStaff(LoginRequest request) {
        return loginStaffInternal(request);
    }

    private ResponseEntity<BaseResponse> login(LoginRequest request, RoleEnum role) {
        try {
            String usernameWithRole = request.getEmail() + ":" + role.name();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameWithRole, request.getPassword())
            );

            Account user = repository.findByEmailAndRole(request.getEmail(), role)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with this role"));

            // ✅ NEW: CHỈ CHECK isActive
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new BaseResponse<>(403,
                                "Tài khoản chưa được kích hoạt hoặc đang bị khóa. Vui lòng kiểm tra email xác nhận hoặc liên hệ quản trị viên.",
                                null));
            }

            var customerOpt = customerRepository.findByAccount_Id(user.getId());
            UUID customerId = customerOpt.map(Customer::getId).orElse(null);

            String accessToken = jwtTokenProvider.generateToken(
                    user.getId(), customerId, user.getEmail(), user.getRole().name()
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(), customerId, user.getEmail(), user.getRole().name()
            );

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            LoginResponse loginResponse =
                    new LoginResponse(accessToken, refreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }

    private ResponseEntity<BaseResponse> loginStaffInternal(LoginRequest request) {
        try {
            String usernameWithRole = request.getEmail() + ":" + RoleEnum.STAFF.name();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameWithRole, request.getPassword())
            );

            Account user = repository.findByEmailAndRole(request.getEmail(), RoleEnum.STAFF)
                    .orElseThrow(() -> new UsernameNotFoundException("Staff account not found"));

            // ✅ NEW: CHỈ CHECK isActive
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new BaseResponse<>(403,
                                "Tài khoản chưa được kích hoạt hoặc đang bị khóa. Vui lòng liên hệ quản trị viên.",
                                null));
            }

            var staffOpt = staffRepository.findByAccount_Id(user.getId());
            if (staffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BaseResponse<>(401, "Staff profile not found for this account", null));
            }

            var staff = staffOpt.get();
            UUID staffId = staff.getId();
            UUID storeId = (staff.getStore() != null) ? staff.getStore().getStoreId() : null;

            String accessToken = jwtTokenProvider.generateToken(
                    user.getId(), null, user.getEmail(), user.getRole().name()
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(), null, user.getEmail(), user.getRole().name()
            );

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            StaffInfo staffInfo =
                    new StaffInfo(staffId, storeId, staff.getFullName(), staff.getEmail(), staff.getPhone());

            LoginResponse loginResponse =
                    new LoginResponse(accessToken, refreshToken, userResponse, staffInfo);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login staff success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }

    // =====================================================
    // VERIFY EMAIL
    // =====================================================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> verifyEmail(String token) {
        try {
            Account acc = repository.findByEmailVerifyToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));

            if (acc.getEmailVerifyTokenExpiry() == null ||
                    LocalDateTime.now().isAfter(acc.getEmailVerifyTokenExpiry())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Token đã hết hạn. Vui lòng yêu cầu gửi lại email xác nhận.", null));
            }

            // ✅ NEW: verify -> ACTIVE (không cần check emailVerified)
            acc.setActive(true);

            // (giữ lại để lưu lịch sử, không dùng để check login nữa)
            acc.setEmailVerified(true);

            acc.setEmailVerifyToken(null);
            acc.setEmailVerifyTokenExpiry(null);
            repository.save(acc);

            return ResponseEntity.ok(new BaseResponse<>(200, "Kích hoạt tài khoản thành công", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(400, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Verify email failed: " + ex.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> resendVerifyEmail(ResendVerifyEmailRequest request) {
        try {
            if (request.getRole() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "role is required", null));
            }

            Account acc = repository.findByEmailAndRole(request.getEmail(), request.getRole())
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

            // ✅ NEW: chỉ cần check active (active rồi thì khỏi resend)
            if (acc.isActive()) {
                return ResponseEntity.ok(new BaseResponse<>(200, "Tài khoản đã được kích hoạt rồi", null));
            }

            issueEmailVerifyToken(acc);
            sendVerifyEmailSafe(acc);

            return ResponseEntity.ok(new BaseResponse<>(200, "Đã gửi lại email xác nhận", null));
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BaseResponse<>(404, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Resend verify email failed: " + ex.getMessage(), null));
        }
    }

    // =====================================================
    // REFRESH TOKEN
    // =====================================================
    @Override
    public ResponseEntity<BaseResponse> refreshToken(RefreshTokenRequest request) {
        try {
            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BaseResponse<>(401, "Invalid refresh token", null));
            }

            UUID accountId = jwtTokenProvider.getAccountIdFromRefreshToken(request.getRefreshToken());
            UUID customerId = jwtTokenProvider.getCustomerIdFromRefreshToken(request.getRefreshToken());
            String email = jwtTokenProvider.getEmailFromRefreshToken(request.getRefreshToken());
            String role = jwtTokenProvider.getRoleFromRefreshToken(request.getRefreshToken());

            Account user = repository.findById(accountId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String newAccessToken = jwtTokenProvider.generateToken(accountId, customerId, email, role);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(accountId, customerId, email, role);

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            LoginResponse loginResponse =
                    new LoginResponse(newAccessToken, newRefreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Token refreshed successfully", loginResponse));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid refresh token", null));
        }
    }

    // =====================================================
    // FORGOT / RESET PASSWORD
    // =====================================================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> forgotPassword(ForgotPasswordRequest request) {
        try {
            Account account = repository.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Email không tồn tại trong hệ thống"));

            String resetToken = UUID.randomUUID().toString();

            account.setResetPasswordToken(resetToken);
            account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
            repository.save(account);

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            AccountData emailData = new AccountData(
                    account.getEmail(),
                    account.getName(),
                    account.getRole().toString(),
                    resetLink
            );

            try {
                emailService.sendEmail(EmailTemplateType.RESET_PASSWORD, emailData);
            } catch (MessagingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new BaseResponse<>(500, "Gửi email reset password thất bại: " + e.getMessage(), null));
            }

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "Email reset password đã được gửi thành công. Vui lòng kiểm tra hộp thư của bạn.", null)
            );

        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BaseResponse<>(404, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Có lỗi xảy ra khi gửi email: " + ex.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> resetPassword(ResetPasswordRequest request) {
        try {
            Account account = repository.findByResetPasswordToken(request.getToken())
                    .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn"));

            if (account.getResetPasswordTokenExpiry() == null ||
                    LocalDateTime.now().isAfter(account.getResetPasswordTokenExpiry())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Token đã hết hạn. Vui lòng yêu cầu reset password lại.", null));
            }

            account.setPassword(passwordEncoder.encode(request.getNewPassword()));
            account.setResetPasswordToken(null);
            account.setResetPasswordTokenExpiry(null);
            repository.save(account);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "Mật khẩu đã được đặt lại thành công. Bạn có thể đăng nhập với mật khẩu mới.", null)
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(400, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Có lỗi xảy ra khi đặt lại mật khẩu: " + ex.getMessage(), null));
        }
    }

    // =====================================================
    // FIREBASE reset / register / login
    // =====================================================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> resetPasswordByFirebase(ResetPasswordByFirebaseRequest request) {
        try {
            var firebaseToken = firebaseAuthService.verifyIdToken(request.getFirebaseIdToken());

            Object phoneObj = firebaseToken.getClaims().get("phone_number");
            String phoneFromFirebase = phoneObj != null ? phoneObj.toString() : null;

            if (phoneFromFirebase == null || phoneFromFirebase.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Firebase token không chứa số điện thoại", null));
            }

            String phoneForDb = normalizePhoneForCompare(phoneFromFirebase);

            Account account = repository.findByPhone(phoneForDb)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account với SĐT này"));

            account.setPassword(passwordEncoder.encode(request.getNewPassword()));
            repository.save(account);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "Mật khẩu đã được đặt lại thành công. Bạn có thể đăng nhập với mật khẩu mới.", null)
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(400, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Có lỗi xảy ra khi đặt lại mật khẩu: " + ex.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> verifyPhoneForRegister(VerifyPhoneForRegisterRequest request) {
        try {
            var firebaseToken = firebaseAuthService.verifyIdToken(request.getFirebaseIdToken());

            Object phoneObj = firebaseToken.getClaims().get("phone_number");
            String phoneE164 = phoneObj != null ? phoneObj.toString() : null;

            if (phoneE164 == null || phoneE164.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Firebase token không chứa số điện thoại", null));
            }

            String phoneForDb = normalizePhoneForCompare(phoneE164);

            if (repository.existsByPhone(phoneForDb)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Phone number already used", null));
            }

            String ticket = jwtTokenProvider.generateRegisterTicket(phoneForDb, request.getRole().name(), 2);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "Phone verified", new RegisterPhoneVerifiedResponse(ticket))
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(400, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Verify phone failed: " + ex.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponse> completeRegister(CompleteRegisterRequest request, String registerTicket) {
        try {
            var claims = jwtTokenProvider.parseRegisterTicket(registerTicket);

            String phone = claims.get("phone", String.class);
            String roleStr = claims.get("role", String.class);

            if (phone == null || roleStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Register ticket thiếu thông tin", null));
            }

            RoleEnum role = RoleEnum.valueOf(roleStr);

            if (repository.existsByEmailAndRole(request.getEmail(), role)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Email already used with role " + role, null));
            }

            if (repository.existsByPhone(phone)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Phone number already used", null));
            }

            Account entity = Account.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .phone(phone)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .emailVerified(false)
                    .build();

            // ✅ NEW: Customer + Store đăng ký xong -> inactive cho tới khi verify
            if (role == RoleEnum.CUSTOMER || role == RoleEnum.STOREOWNER) {
                entity.setActive(false);
            } else {
                entity.setActive(true);
            }

            repository.save(entity);

            // issue + send verify email
            issueEmailVerifyToken(entity);
            sendVerifyEmailSafe(entity);

            createDefaultCustomerForAccount(entity);
            if (role == RoleEnum.STOREOWNER) createDefaultStoreWithWallet(entity);

            RegisterResponse response =
                    new RegisterResponse(entity.getEmail(), entity.getName(), entity.getPhone());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new BaseResponse<>(201, "Register success", response));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(400, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(500, "Register failed: " + ex.getMessage(), null));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseResponse> loginByPhoneFirebase(LoginByPhoneFirebaseRequest request) {
        try {
            var firebaseToken = firebaseAuthService.verifyIdToken(request.getFirebaseIdToken());

            Object phoneObj = firebaseToken.getClaims().get("phone_number");
            String phoneFromFirebase = phoneObj != null ? phoneObj.toString() : null;

            if (phoneFromFirebase == null || phoneFromFirebase.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Firebase token không chứa số điện thoại", null));
            }

            String phoneForDb = normalizePhoneForCompare(phoneFromFirebase);

            Account user = repository.findByPhone(phoneForDb)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy account với SĐT này"));

            // ✅ NEW: CHỈ CHECK isActive
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new BaseResponse<>(403,
                                "Tài khoản chưa được kích hoạt hoặc đang bị khóa. Vui lòng kiểm tra email xác nhận hoặc liên hệ quản trị viên.",
                                null));
            }

            var customerOpt = customerRepository.findByAccount_Id(user.getId());
            UUID customerId = customerOpt.map(Customer::getId).orElse(null);

            String accessToken = jwtTokenProvider.generateToken(
                    user.getId(), customerId, user.getEmail(), user.getRole().name()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(), customerId, user.getEmail(), user.getRole().name()
            );

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            LoginResponse loginResponse =
                    new LoginResponse(accessToken, refreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login by phone success", loginResponse));

        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BaseResponse<>(404, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Firebase token invalid: " + ex.getMessage(), null));
        }
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private void issueEmailVerifyToken(Account acc) {
        String token = UUID.randomUUID().toString();
        acc.setEmailVerifyToken(token);
        acc.setEmailVerifyTokenExpiry(LocalDateTime.now().plusHours(24));

        // vẫn giữ field này nếu bạn muốn lưu info, nhưng login không check nữa
        acc.setEmailVerified(false);

        repository.save(acc);
    }

    private void sendVerifyEmailSafe(Account acc) {
        try {
            sendVerifyEmail(acc);
        } catch (MessagingException e) {
            System.err.println("⚠️ Send verify email failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Send verify email failed (unknown): " + e.getMessage());
        }
    }

    private void sendVerifyEmail(Account acc) throws MessagingException {
        String verifyLink = frontendUrl + "/verify-account?token=" + acc.getEmailVerifyToken();

        AccountVerifyData data = AccountVerifyData.builder()
                .email(acc.getEmail())
                .name(acc.getName())
                .role(acc.getRole().name())
                .verifyLink(verifyLink)
                .siteUrl(frontendUrl)
                .build();

        emailService.sendEmail(EmailTemplateType.ACCOUNT_VERIFY, data);
    }

    private void createDefaultCustomerForAccount(Account account) {
        if (customerRepository.existsByAccount_Id(account.getId())) return;

        String defaultUsername = account.getEmail().split("@")[0];

        Customer customer = Customer.builder()
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

        customerRepository.save(customer);
        createDefaultWalletForCustomer(customer);
    }

    private void createDefaultWalletForCustomer(Customer customer) {
        if (walletRepository.existsByCustomer_Id(customer.getId())) return;
        Wallet wallet = Wallet.builder()
                .customer(customer)
                .build();
        walletRepository.save(wallet);
    }

    private void createDefaultStoreWithWallet(Account account) {
        if (storeRepository.existsByAccount_Id(account.getId())) return;

        Store store = Store.builder()
                .account(account)
                .storeName(account.getName())
                .description("This store is created automatically and is inactive until KYC is approved.")
                .status(StoreStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .email(account.getEmail())
                .phoneNumber(account.getPhone())
                .build();

        storeRepository.save(store);

        StoreWallet wallet = StoreWallet.builder()
                .store(store)
                .availableBalance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .totalRevenue(BigDecimal.ZERO)
                .depositBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        storeWalletRepository.save(wallet);

        store.setWallet(wallet);
        storeRepository.save(store);

        StoreWalletTransaction initTransaction = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.ADJUSTMENT)
                .amount(BigDecimal.ZERO)
                .balanceAfter(BigDecimal.ZERO)
                .description("📦 Ví cửa hàng được tạo tự động khi đăng ký tài khoản Store Owner")
                .createdAt(LocalDateTime.now())
                .build();

        storeWalletTransactionRepository.save(initTransaction);
    }

    private String normalizePhoneForCompare(String phone) {
        if (phone == null) return null;
        phone = phone.trim().replaceAll("\\s+", "");

        if (phone.startsWith("+84")) return "0" + phone.substring(3);
        if (phone.startsWith("84")) return "0" + phone.substring(2);
        return phone;
    }
}
