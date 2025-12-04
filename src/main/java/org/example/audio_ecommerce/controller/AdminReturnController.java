package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ReturnDisputeResolveRequest;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.service.CustomerReturnComplaintService;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnRequestService returnService;
    private final CustomerReturnComplaintService complaintService;

    @GetMapping("/disputes")
    public Page<ReturnRequestResponse> listDispute(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listDispute(pageable);
    }

    @PostMapping("/{id}/resolve-dispute")
    public ReturnRequestResponse resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnDisputeResolveRequest req
    ) {
        return returnService.resolveDispute(id, req);
    }

    @PostMapping("/complaints/auto-refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processAutoRefundComplaints() {
        complaintService.processAutoRefundComplaints();
    }
}
