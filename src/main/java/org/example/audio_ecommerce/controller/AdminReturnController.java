package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(
            summary = "Admin xem danh sách return đang dispute",
            description = """
                ✅ FE Admin dùng cho màn 'Dispute queue'.
                
                Chỉ trả các ReturnRequest có status = DISPUTE_ESCALATED (hoặc các status dispute).
                Pagination: page/size.
                """
    )
    @GetMapping("/disputes")
    public Page<ReturnRequestResponse> listDispute(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listDispute(pageable);
    }

    @Operation(
            summary = "Admin xử lý dispute (phán quyết)",
            description = """
                ✅ FE Admin dùng khi bấm Resolve.
                
                Input (ReturnDisputeResolveRequest):
                - decision: RESOLVED_SHOP / RESOLVED_CUSTOMER (tuỳ enum bạn đang dùng)
                - note/reason
                
                Action:
                - Nếu resolved customer: hệ thống refund theo rule
                - Nếu resolved shop: có thể close case / reject refund
                - Có thể trigger legalPoint / penalty nếu bạn có logic
                
                Output: ReturnRequestResponse cập nhật status cuối.
                """
    )
    @PostMapping("/{id}/resolve-dispute")
    public ReturnRequestResponse resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnDisputeResolveRequest req
    ) {
        return returnService.resolveDispute(id, req);
    }

    @Operation(
            summary = "Admin trigger xử lý auto-refund complaints",
            description = """
                ✅ Dùng cho:
                - cron job / manual trigger
                - auto refund các complaint quá hạn SLA
                
                Response: 204 NO_CONTENT
                """
    )
    @PostMapping("/complaints/auto-refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processAutoRefundComplaints() {
        complaintService.processAutoRefundComplaints();
    }
}
