package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.*;
import org.example.audio_ecommerce.dto.response.ReturnPackageFeeResponse;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReturnRequestService {

    // CUSTOMER
    ReturnRequestResponse createReturnRequest(ReturnRequestCreateRequest req);

    Page<ReturnRequestResponse> listForCurrentCustomer(Pageable pageable);

    ReturnPackageFeeResponse setPackageInfoAndCalculateFee(
            UUID returnRequestId,
            ReturnPackageInfoRequest req
    );

    // SHOP
    Page<ReturnRequestResponse> listForCurrentShop(Pageable pageable);

    void approveReturnByShop(UUID returnRequestId);

    ReturnRequestResponse createGhnReturnOrderByShop(UUID returnRequestId, ReturnCreateGhnOrderRequest req);

    ReturnRequestResponse shopReceiveOrDispute(
            UUID returnRequestId,
            ReturnShopReceiveRequest req
    );

    // ADMIN
    Page<ReturnRequestResponse> listDispute(Pageable pageable);

    ReturnRequestResponse resolveDispute(
            UUID returnRequestId,
            ReturnDisputeResolveRequest req
    );
    void rejectReturnByShop(UUID returnRequestId, ReturnRejectRequest req);

    // AUTO
    void autoRefundForUnresponsiveShop();
    void autoApprovePendingReturns();
    void autoCancelUnshippedReturns();
    void autoHandleGhnPickupTimeout();

    ReturnRequestResponse refundWithoutReturnByShop(UUID returnRequestId);

}
