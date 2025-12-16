package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.AdminDecisionRequest;
import org.example.audio_ecommerce.dto.request.AdminWithdrawMarkPaidRequest;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminWithdrawService {
    Page<CustomerWithdrawResponse> adminList(WithdrawRequestStatus status, int page, int size);

    CustomerWithdrawResponse adminGet(UUID withdrawRequestId);

    CustomerWithdrawResponse approve(UUID withdrawRequestId, AdminDecisionRequest req);

    CustomerWithdrawResponse reject(UUID withdrawRequestId, AdminDecisionRequest req);

    CustomerWithdrawResponse markPaid(UUID withdrawRequestId, AdminWithdrawMarkPaidRequest req);
}
