package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

public interface AccountService {
    // Customer
    ResponseEntity<BaseResponse> registerCustomer(RegisterRequest request);

    ResponseEntity<BaseResponse> loginCustomer(LoginRequest request);

    // Store
    ResponseEntity<BaseResponse> registerStore(RegisterRequest request);

    ResponseEntity<BaseResponse> loginStore(LoginRequest request);

    // Admin
    ResponseEntity<BaseResponse> registerAdmin(RegisterRequest request);

    ResponseEntity<BaseResponse> loginAdmin(LoginRequest request);

    // Refresh Token
    ResponseEntity<BaseResponse> refreshToken(RefreshTokenRequest request);


    ResponseEntity<BaseResponse> registerFlatStaff(RegisterRequest request);

    ResponseEntity<BaseResponse> loginFlatStaff(LoginRequest request);


    //Staff
    ResponseEntity<BaseResponse> loginStaff(LoginRequest request);

    // Reset Password
    ResponseEntity<BaseResponse> forgotPassword(ForgotPasswordRequest request);

    ResponseEntity<BaseResponse> resetPassword(ResetPasswordRequest request);

    //firebase otp
    ResponseEntity<BaseResponse> resetPasswordByFirebase(
            ResetPasswordByFirebaseRequest request);
    //firebase register
    ResponseEntity<BaseResponse> verifyPhoneForRegister(VerifyPhoneForRegisterRequest request);

    ResponseEntity<BaseResponse> completeRegister(CompleteRegisterRequest request, String registerTicket);

    //firebase login
    ResponseEntity<BaseResponse> loginByPhoneFirebase(LoginByPhoneFirebaseRequest request);

}
