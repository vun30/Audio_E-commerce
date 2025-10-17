package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ShopVoucherService {
    ResponseEntity<BaseResponse> createVoucher(ShopVoucherRequest req);
    ResponseEntity<BaseResponse> getAllVouchers();
    ResponseEntity<BaseResponse> getVoucherById(UUID id);
    ResponseEntity<BaseResponse> disableVoucher(UUID id);
}
