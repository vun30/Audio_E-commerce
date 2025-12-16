package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.email.AccountData;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.security.JwtTokenProvider;
import org.example.audio_ecommerce.service.AccountService;
import org.example.audio_ecommerce.email.EmailService;
import org.example.audio_ecommerce.email.EmailTemplateType;
import org.example.audio_ecommerce.service.FirebaseAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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

    // 👇 thêm dependency EmailService
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

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

        // ✅ 1️⃣ Tạo tài khoản
        Account entity = Account.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        repository.save(entity);

        // ✅ 2️⃣ Tạo Customer mặc định
        createDefaultCustomerForAccount(entity);

        // ✅ 3️⃣ Nếu role là STOREOWNER → tạo store + ví + transaction mặc định
        if (role == RoleEnum.STOREOWNER) {
            createDefaultStoreWithWallet(entity);
        }

        // ✅ 4️⃣ Gửi email chào mừng theo role
        try {
            AccountData mailData = new AccountData(
                    entity.getEmail(),
                    entity.getName(),
                    role.name(),               // CUSTOMER / STOREOWNER / ADMIN
                    "https://www.facebook.com/hoan.vu.3012" // đường link trang web (sửa theo domain của bạn)
            );

            emailService.sendEmail(EmailTemplateType.ACCOUNT_WELCOME, mailData);
        } catch (Exception e) {
            System.err.println("⚠️ Gửi mail chào mừng thất bại: " + e.getMessage());
        }

        RegisterResponse response = new RegisterResponse(entity.getEmail(), entity.getName(), entity.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(201, successMsg, response));
    }

    // ==================== Helpers ====================

    /**
     * ✅ Luôn tạo hồ sơ Customer 1–1 cho Account
     */
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

    /**
     * ✅ Tạo store + store_wallet + transaction mặc định khi đăng ký Store Owner
     */
    private void createDefaultStoreWithWallet(Account account) {
        if (storeRepository.existsByAccount_Id(account.getId())) return;

        // 1️⃣ Tạo Store mặc định
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

        // 2️⃣ Tạo Store Wallet mặc định
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

        // Gán ví vào store và lưu lại (quan hệ 1-1 hai chiều)
        store.setWallet(wallet);
        storeRepository.save(store);

        // 3️⃣ Tạo giao dịch khởi tạo ví
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

            var customerOpt = customerRepository.findByAccount_Id(user.getId());
            UUID customerId = customerOpt.map(Customer::getId).orElse(null);
            String accessToken = jwtTokenProvider.generateToken(user.getId(), customerId, user.getEmail(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), customerId, user.getEmail(), user.getRole().name());
            AccountResponse userResponse = new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }

    // ==================== REFRESH TOKEN ====================
    @Override
    public ResponseEntity<BaseResponse> refreshToken(RefreshTokenRequest request) {
        try {
            // Validate refresh token
            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BaseResponse<>(401, "Invalid refresh token", null));
            }

            // Extract information from refresh token
            UUID accountId = jwtTokenProvider.getAccountIdFromRefreshToken(request.getRefreshToken());
            UUID customerId = jwtTokenProvider.getCustomerIdFromRefreshToken(request.getRefreshToken());
            String email = jwtTokenProvider.getEmailFromRefreshToken(request.getRefreshToken());
            String role = jwtTokenProvider.getRoleFromRefreshToken(request.getRefreshToken());

            // Verify account still exists
            Account user = repository.findById(accountId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateToken(accountId, customerId, email, role);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(accountId, customerId, email, role);

            // Create response
            AccountResponse userResponse = new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());
            LoginResponse loginResponse = new LoginResponse(newAccessToken, newRefreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Token refreshed successfully", loginResponse));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid refresh token", null));
        }
    }

    @Override

    public ResponseEntity<BaseResponse> registerFlatStaff(RegisterRequest request) {
        return register(request, RoleEnum.FLATSTAFF, "Flat Staff created");
    }

    @Override
    public ResponseEntity<BaseResponse> loginFlatStaff(LoginRequest request) {
        return login(request, RoleEnum.FLATSTAFF);
    }

    public ResponseEntity<BaseResponse> loginStaff(LoginRequest request) {
        return loginStaffInternal(request);
    }

    private ResponseEntity<BaseResponse> loginStaffInternal(LoginRequest request) {
        try {
            // Ghép email với role STAFF theo chuẩn bạn đang dùng
            String usernameWithRole = request.getEmail() + ":" + RoleEnum.STAFF.name();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameWithRole, request.getPassword())
            );

            Account user = repository.findByEmailAndRole(request.getEmail(), RoleEnum.STAFF)
                    .orElseThrow(() -> new UsernameNotFoundException("Staff account not found"));

            // Tìm hồ sơ Staff
            var staffOpt = staffRepository.findByAccount_Id(user.getId());
            if (staffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new BaseResponse<>(401, "Staff profile not found for this account", null));
            }

            var staff = staffOpt.get();
            UUID staffId = staff.getId();
            UUID storeId = (staff.getStore() != null) ? staff.getStore().getStoreId() : null;

            // ✅ Tạo token — giữ tương thích: vẫn generate theo (accountId, customerId, email, role)
            // Với staff thì customerId = null
            String accessToken = jwtTokenProvider.generateToken(user.getId(), null, user.getEmail(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), null, user.getEmail(), user.getRole().name());

            AccountResponse userResponse =
                    new AccountResponse(user.getEmail(), user.getName(), user.getRole().toString());

            StaffInfo staffInfo = new StaffInfo(staffId, storeId, staff.getFullName(), staff.getEmail(), staff.getPhone());

            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userResponse, staffInfo);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login staff success", loginResponse));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Invalid credentials", null));
        }
    }

    // ==================== FORGOT PASSWORD ====================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> forgotPassword(ForgotPasswordRequest request) {
        try {
            // Tìm account theo email (không phân biệt role)
            Account account = repository.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Email không tồn tại trong hệ thống"));

            // Tạo reset token (UUID)
            String resetToken = UUID.randomUUID().toString();

            // Set token và thời gian hết hạn (30 phút)
            account.setResetPasswordToken(resetToken);
            account.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));

            // Lưu vào database
            repository.save(account);

            // Tạo link reset password
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            // Gửi email
            AccountData emailData = new AccountData(
                    account.getEmail(),
                    account.getName(),
                    account.getRole().toString(),
                    resetLink
            );

            emailService.sendEmail(EmailTemplateType.RESET_PASSWORD, emailData);

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

    // ==================== RESET PASSWORD ====================
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> resetPassword(ResetPasswordRequest request) {
        try {
            // Tìm account theo reset token
            Account account = repository.findByResetPasswordToken(request.getToken())
                    .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn"));

            // Kiểm tra token có hết hạn chưa
            if (account.getResetPasswordTokenExpiry() == null ||
                LocalDateTime.now().isAfter(account.getResetPasswordTokenExpiry())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Token đã hết hạn. Vui lòng yêu cầu reset password lại.", null));
            }

            // Cập nhật password mới
            account.setPassword(passwordEncoder.encode(request.getNewPassword()));

            // Xóa reset token và expiry
            account.setResetPasswordToken(null);
            account.setResetPasswordTokenExpiry(null);

            // Lưu vào database
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

    //firebase otp
    @Override
    @Transactional
    public ResponseEntity<BaseResponse> resetPasswordByFirebase(ResetPasswordByFirebaseRequest request) {
        try {
            // 1) Verify Firebase ID token
            var firebaseToken = firebaseAuthService.verifyIdToken(request.getFirebaseIdToken());

            // 2) Lấy phone từ claims
            Object phoneObj = firebaseToken.getClaims().get("phone_number");
            String phoneFromFirebase = phoneObj != null ? phoneObj.toString() : null;

            if (phoneFromFirebase == null || phoneFromFirebase.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Firebase token không chứa số điện thoại", null));
            }

            // 3) Chuẩn hoá để match DB (0xxx)
            String phoneForDb = normalizePhoneForCompare(phoneFromFirebase);

            // 4) Tìm account theo phone
            Account account = repository.findByPhone(phoneForDb)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy account với SĐT này"));

            // 5) Cập nhật password mới
            account.setPassword(passwordEncoder.encode(request.getNewPassword()));

            // 6) Lưu vào database
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


    private String normalizePhoneForCompare(String phone) {
        if (phone == null) return null;
        phone = phone.trim().replaceAll("\\s+", "");

        if (phone.startsWith("+84")) return "0" + phone.substring(3);
        if (phone.startsWith("84"))  return "0" + phone.substring(2);
        return phone; // nếu đã là 0xxx thì giữ nguyên
    }

    //firebase register
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

            // Nếu DB đang lưu 0xxx thì convert sang 0xxx để check duplicate theo DB hiện tại
            String phoneForDb = normalizePhoneForCompare(phoneE164);

            // Check đã tồn tại account theo phone chưa
            if (repository.existsByPhone(phoneForDb)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Phone number already used", null));
            }

            // Tạo ticket sống 10 phút
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

            // Check email + role
            if (repository.existsByEmailAndRole(request.getEmail(), role)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Email already used with role " + role, null));
            }

            // Check phone
            if (repository.existsByPhone(phone)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new BaseResponse<>(409, "Phone number already used", null));
            }

            Account entity = Account.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .phone(phone) // phone đã verify
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .build();

            repository.save(entity);

            // tạo customer default + store wallet như bạn đang làm
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

    //login sdt firebase
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseResponse> loginByPhoneFirebase(LoginByPhoneFirebaseRequest request) {
        try {
            // 1) Verify Firebase token
            var firebaseToken = firebaseAuthService.verifyIdToken(request.getFirebaseIdToken());

            // 2) Lấy phone từ claims
            Object phoneObj = firebaseToken.getClaims().get("phone_number");
            String phoneFromFirebase = phoneObj != null ? phoneObj.toString() : null;

            if (phoneFromFirebase == null || phoneFromFirebase.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(400, "Firebase token không chứa số điện thoại", null));
            }

            // 3) Chuẩn hoá để match DB (vì DB bạn đang lưu 0xxx)
            String phoneForDb = normalizePhoneForCompare(phoneFromFirebase);

            // 4) Tìm account theo phone
            Account user = repository.findByPhone(phoneForDb)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy account với SĐT này"));

            // 5) Tạo JWT của hệ thống bạn
            // customerId có nếu role CUSTOMER
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

            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userResponse);

            return ResponseEntity.ok(new BaseResponse<>(200, "Login by phone success", loginResponse));

        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BaseResponse<>(404, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "Firebase token invalid: " + ex.getMessage(), null));
        }
    }





}
