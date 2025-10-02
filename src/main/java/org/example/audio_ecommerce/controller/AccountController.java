package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.LoginRequest;
import org.example.audio_ecommerce.dto.request.RegisterRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ========================= REGISTER =========================
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

    // ========================= LOGIN =========================
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
}
