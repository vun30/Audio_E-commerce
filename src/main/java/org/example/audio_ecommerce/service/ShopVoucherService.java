package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.request.ShopWideVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ShopVoucherResponse;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ShopVoucherService {
    ResponseEntity<BaseResponse<ShopVoucherResponse>> createVoucher(ShopVoucherRequest req);
    ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getAllVouchers();
    ResponseEntity<BaseResponse<ShopVoucherResponse>> getVoucherById(UUID id);
    ResponseEntity<BaseResponse<ShopVoucherResponse>> disableVoucher(UUID id);
    ResponseEntity<BaseResponse<ShopVoucherResponse>> getActiveVoucherByProductId(UUID productId);
    ResponseEntity<BaseResponse<ShopVoucherResponse>> createShopWideVoucher(ShopWideVoucherRequest req);
    ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getActiveVouchersByType(VoucherStatus status, ShopVoucherScopeType scopeType);
    ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getVouchersByStore(UUID storeId, VoucherStatus status, ShopVoucherScopeType scopeType);
    ResponseEntity<BaseResponse<String>> generateVoucherCode();
}
