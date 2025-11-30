package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.request.ShopWideVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ShopVoucherService {
    ResponseEntity<BaseResponse> createVoucher(ShopVoucherRequest req);
    ResponseEntity<BaseResponse> getAllVouchers();
    ResponseEntity<BaseResponse> getVoucherById(UUID id);
    ResponseEntity<BaseResponse> disableVoucher(UUID id);
    ResponseEntity<BaseResponse> getActiveVoucherByProductId(UUID productId);
    ResponseEntity<BaseResponse> createShopWideVoucher(ShopWideVoucherRequest req);
    ResponseEntity<BaseResponse> getActiveVouchersByType(VoucherStatus status, ShopVoucherScopeType scopeType);
    ResponseEntity<BaseResponse> getVouchersByStore(UUID storeId, VoucherStatus status, ShopVoucherScopeType scopeType);

}
