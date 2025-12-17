package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CustomerWithdrawCreateRequest;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CustomerWithdrawService {
    CustomerWithdrawRequest create(UUID customerId, CustomerWithdrawCreateRequest req);
    Page<CustomerWithdrawResponse> customerList(UUID customerId, WithdrawRequestStatus status, int page, int size);
    CustomerWithdrawResponse customerGet(UUID customerId, UUID withdrawRequestId); // add proof urls
}
