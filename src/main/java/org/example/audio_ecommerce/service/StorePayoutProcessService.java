package org.example.audio_ecommerce.service;


import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;

public interface StorePayoutProcessService {
    ResponseEntity<BaseResponse> processMyEligiblePayoutItems();
}
