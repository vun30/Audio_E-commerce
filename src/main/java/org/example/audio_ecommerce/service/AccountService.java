package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RefreshTokenRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
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
}
