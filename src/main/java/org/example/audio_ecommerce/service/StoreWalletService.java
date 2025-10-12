package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

public interface StoreWalletService {
    /**
     * ✅ Lấy thông tin ví của cửa hàng đang đăng nhập
     */
    ResponseEntity<BaseResponse> getMyWallet();

    ResponseEntity<BaseResponse> getMyWalletTransactions(int page, int size, String type);
}
