package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.ShippingMethodRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ShippingMethodService {
    ResponseEntity<BaseResponse> create(ShippingMethodRequest request);
    ResponseEntity<BaseResponse> update(UUID id, ShippingMethodRequest request);
    ResponseEntity<BaseResponse> delete(UUID id);
    ResponseEntity<BaseResponse> getAll();
    ResponseEntity<BaseResponse> getById(UUID id);
}
