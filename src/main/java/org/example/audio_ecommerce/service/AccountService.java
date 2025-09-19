package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

public interface AccountService {
    ResponseEntity<BaseResponse> create(RegisterRequest request);
    ResponseEntity<BaseResponse> login(LoginRequest request);
}
