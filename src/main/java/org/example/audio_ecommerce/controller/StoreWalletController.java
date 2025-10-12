package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores/me/wallet")
@RequiredArgsConstructor
public class StoreWalletController {

    private final StoreWalletService storeWalletService;

    /**
     * ✅ Lấy thông tin ví của cửa hàng đang đăng nhập
     */
    @GetMapping
    public ResponseEntity<BaseResponse> getMyWallet() {
        return storeWalletService.getMyWallet();
    }


    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse> getMyWalletTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type // ví dụ: DEPOSIT, WITHDRAW, REFUND
    ) {
        return storeWalletService.getMyWalletTransactions(page, size, type);
    }
}
