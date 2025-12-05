package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ReturnCreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.request.ReturnRejectRequest;
import org.example.audio_ecommerce.dto.request.ReturnShopReceiveRequest;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/store/returns")
@RequiredArgsConstructor
public class StoreReturnController {

    private final ReturnRequestService returnService;

    @GetMapping
    public Page<ReturnRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listForCurrentShop(pageable);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable UUID id) {
        returnService.approveReturnByShop(id);
    }

    @PostMapping("/{id}/create-ghn-order")
    public ReturnRequestResponse createGhnOrder(@PathVariable UUID id,@RequestBody(required = false) ReturnCreateGhnOrderRequest req) {
        return returnService.createGhnReturnOrderByShop(id, req);
    }

    @PostMapping("/{id}/receive-or-dispute")
    public ReturnRequestResponse shopReceiveOrDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnShopReceiveRequest req
    ) {
        return returnService.shopReceiveOrDispute(id, req);
    }

    @PostMapping("/{id}/reject")
    public void reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ReturnRejectRequest req
    ) {
        returnService.rejectReturnByShop(id, req);
    }

    @PostMapping("/{id}/refund-without-return")
    public ReturnRequestResponse refundWithoutReturn(@PathVariable UUID id) {
        return returnService.refundWithoutReturnByShop(id);
    }


}
