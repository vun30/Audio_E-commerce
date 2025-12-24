package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // =====================================================
    // 🧩 REGISTER (theo từng role)
    // =====================================================

    @PostMapping("/register/customer")
    public ResponseEntity<BaseResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
        return accountService.registerCustomer(request);
    }

    @PostMapping("/register/store")
    public ResponseEntity<BaseResponse> registerStore(@Valid @RequestBody RegisterRequest request) {
        return accountService.registerStore(request);
    }

    @PostMapping("/register/admin")
    public ResponseEntity<BaseResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return accountService.registerAdmin(request);
    }

    @PostMapping("/register/flatstaff")
    public ResponseEntity<BaseResponse> registerFlatStaff(@Valid @RequestBody RegisterRequest request) {
        return accountService.registerFlatStaff(request);
    }

    // =====================================================
    // 🔐 LOGIN (theo từng role)
    // =====================================================

    @PostMapping("/login/customer")
    public ResponseEntity<BaseResponse> loginCustomer(@Valid @RequestBody LoginRequest request) {
        return accountService.loginCustomer(request);
    }

    @PostMapping("/login/store")
    public ResponseEntity<BaseResponse> loginStore(@Valid @RequestBody LoginRequest request) {
        return accountService.loginStore(request);
    }


    @PostMapping("/login/admin")
    public ResponseEntity<BaseResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return accountService.loginAdmin(request);
    }

    @PostMapping("/login/flatstaff")
    public ResponseEntity<BaseResponse> loginFlatStaff(@Valid @RequestBody LoginRequest request) {
        return accountService.loginFlatStaff(request);
    }


    // =====================================================
    // ♻️ REFRESH TOKEN
    // =====================================================

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return accountService.refreshToken(request);
    }

    // =====================================================
    // 🔑 RESET PASSWORD
    // =====================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return accountService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return accountService.resetPassword(request);
    }
    //firebase
    @Operation(
            summary = "🔐 [PUBLIC] Đặt lại mật khẩu bằng OTP SMS (Firebase)",
            description = """
            ### Đặt lại mật khẩu bằng số điện thoại đã xác thực OTP
            
            API này dùng cho **quên mật khẩu / reset mật khẩu** thông qua **OTP gửi về số điện thoại**.
            
            ⚙️ **Cơ chế hoạt động**
            - OTP **KHÔNG** được backend gửi
            - OTP được **Firebase gửi và xác thực ở Frontend**
            - Frontend gửi **Firebase ID Token** xuống backend
            - Backend verify token → lấy số điện thoại → đổi mật khẩu
            
            ---
            ### 📱 Luồng Frontend
            1. Người dùng nhập **số điện thoại**
            2. Frontend dùng Firebase gửi OTP SMS
            3. Người dùng nhập OTP
            4. Firebase verify OTP thành công
            5. Frontend lấy `firebaseIdToken`
            6. Gọi API này để đặt lại mật khẩu
            
            ---
            ### 📥 Request Body Example
            ```json
            {
              "firebaseIdToken": "eyJhbGciOiJSUzI1NiIs...",
              "newPassword": "NewPassword@123"
            }
            ```
            
            ---
            ### 📌 Lưu ý quan trọng cho Frontend
            - ❌ KHÔNG gửi OTP về backend
            - ❌ KHÔNG gửi số điện thoại
            - ✅ Chỉ gửi `firebaseIdToken`
            - Token phải được tạo **sau khi OTP verify thành công**
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "❌ Token không hợp lệ hoặc thiếu số điện thoại"),
            @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy account với số điện thoại"),
            @ApiResponse(responseCode = "401", description = "❌ Firebase token không hợp lệ / hết hạn")
    })
    @PostMapping("/reset-password/firebase")
    public ResponseEntity<BaseResponse> resetPasswordByFirebase(
            @RequestBody ResetPasswordByFirebaseRequest request) {

        return accountService.resetPasswordByFirebase(request);
    }


    //firebase register
    @Operation(
            summary = "📱 [PUBLIC] Xác thực số điện thoại bằng OTP (Firebase) – Bước 1 đăng ký",
            description = """
            ### Xác thực số điện thoại trước khi đăng ký tài khoản
            
            API này là **bước 1 của luồng đăng ký bằng SĐT + OTP**.
            
            ⚙️ **Cơ chế**
            - OTP được gửi & xác thực hoàn toàn ở **Frontend bằng Firebase**
            - Backend **KHÔNG gửi OTP**
            - Backend chỉ verify `firebaseIdToken`
            
            ---
            ### 🧭 Luồng đăng ký
            **Step 1:** Xác thực số điện thoại (API này)  
            **Step 2:** Nhập thông tin tài khoản & tạo account (`/register/complete`)
            
            ---
            ### 📥 Request Body Example
            ```json
            {
              "firebaseIdToken": "eyJhbGciOiJSUzI1NiIs...",
              "role": "CUSTOMER"
            }
            ```
            
            ---
            ### 📤 Response
            - Backend trả về **registerTicket**
            - Ticket có thời hạn ngắn (5–10 phút)
            - Ticket dùng cho bước `/register/complete`
            
            ```json
            {
              "registerTicket": "eyJhbGciOiJIUzI1NiJ9..."
            }
            ```
            
            ---
            ### 📌 Lưu ý cho Frontend
            - ❌ Không gửi số điện thoại
            - ❌ Không gửi OTP
            - ✅ Chỉ gửi Firebase ID Token
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Số điện thoại hợp lệ, tạo register ticket"),
            @ApiResponse(responseCode = "409", description = "❌ Số điện thoại đã được sử dụng"),
            @ApiResponse(responseCode = "400", description = "❌ Firebase token không hợp lệ")
    })
    @PostMapping("/register/phone/verify")
    public ResponseEntity<BaseResponse> verifyPhoneForRegister(
            @RequestBody VerifyPhoneForRegisterRequest request) {
        return accountService.verifyPhoneForRegister(request);
    }


    @Operation(
            summary = "📝 [PUBLIC] Hoàn tất đăng ký tài khoản bằng SĐT đã xác thực",
            description = """
            ### Hoàn tất đăng ký sau khi xác thực số điện thoại
            
            Đây là **bước 2** trong luồng đăng ký bằng **SĐT + OTP Firebase**.
            
            ---
            ### 🔐 Yêu cầu bắt buộc
            - Frontend phải gọi `/register/phone/verify` trước
            - Lấy `registerTicket` từ response
            - Gửi ticket qua header `X-Register-Ticket`
            
            ---
            ### 📥 Header
            ```
            X-Register-Ticket: eyJhbGciOiJIUzI1NiJ9...
            ```
            
            ---
            ### 📥 Request Body Example
            ```json
            {
              "name": "Nguyễn Văn A",
              "email": "vana@gmail.com",
              "password": "12345678"
            }
            ```
            
            ---
            ### ⚙️ Backend sẽ tự động:
            - Lấy số điện thoại từ ticket
            - Kiểm tra trùng email / phone
            - Tạo Account
            - Tạo Customer + Wallet
            - (Tạo Store nếu role là STOREOWNER)
            
            ---
            ### 📌 Lưu ý
            - ❌ Không gửi số điện thoại
            - ❌ Không gửi OTP
            - ✅ Ticket chỉ dùng 1 lần, có hạn
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "✅ Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "❌ Ticket không hợp lệ / hết hạn"),
            @ApiResponse(responseCode = "409", description = "❌ Email hoặc SĐT đã tồn tại")
    })
    @PostMapping("/register/complete")
    public ResponseEntity<BaseResponse> completeRegister(
            @RequestHeader("X-Register-Ticket") String ticket,
            @RequestBody CompleteRegisterRequest request) {

        return accountService.completeRegister(request, ticket);
    }


    //firebase login
    @Operation(
            summary = "📲 [PUBLIC] Đăng nhập bằng số điện thoại + OTP (Firebase)",
            description = """
            ### Đăng nhập bằng số điện thoại và OTP SMS
            
            API này cho phép người dùng **đăng nhập không cần mật khẩu**, chỉ bằng:
            - Số điện thoại
            - OTP SMS do Firebase gửi
            
            ---
            ### 🔁 Luồng Frontend
            1. Nhập số điện thoại
            2. Firebase gửi OTP
            3. Người dùng nhập OTP
            4. Firebase verify OTP
            5. Frontend lấy `firebaseIdToken`
            6. Gọi API này để login
            
            ---
            ### 📥 Request Body Example
            ```json
            {
              "firebaseIdToken": "eyJhbGciOiJSUzI1NiIs..."
            }
            ```
            
            ---
            ### 📤 Response
            - Backend trả về:
              - accessToken
              - refreshToken
              - thông tin account
            - Dùng giống login email/password
            
            ---
            ### 📌 Lưu ý
            - ❌ Không gửi OTP
            - ❌ Không gửi số điện thoại
            - ✅ Chỉ gửi Firebase ID Token
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Login thành công"),
            @ApiResponse(responseCode = "404", description = "❌ Không tìm thấy account với SĐT"),
            @ApiResponse(responseCode = "401", description = "❌ Firebase token không hợp lệ")
    })
    @PostMapping("/login/phone")
    public ResponseEntity<BaseResponse> loginByPhoneFirebase(
            @RequestBody LoginByPhoneFirebaseRequest request) {
        return accountService.loginByPhoneFirebase(request);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<BaseResponse> verifyEmail(@RequestParam String token) {
        return accountService.verifyEmail(token);
    }

    @PostMapping("/resend-verify-email")
    public ResponseEntity<BaseResponse> resendVerifyEmail(@Valid @RequestBody ResendVerifyEmailRequest request) {
        return accountService.resendVerifyEmail(request);
    }
}
