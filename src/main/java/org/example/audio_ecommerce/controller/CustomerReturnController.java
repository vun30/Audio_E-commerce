package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerReturnComplaintCreateRequest;
import org.example.audio_ecommerce.dto.request.ReturnPackageInfoRequest;
import org.example.audio_ecommerce.dto.request.ReturnRequestCreateRequest;
import org.example.audio_ecommerce.dto.response.ReturnPackageFeeResponse;
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
@RequestMapping("/api/customers/me/returns")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final ReturnRequestService returnService;
    private final CustomerReturnComplaintService complaintService;

    @PostMapping
    public ReturnRequestResponse create(@Valid @RequestBody ReturnRequestCreateRequest req) {
        return returnService.createReturnRequest(req);
    }

    @GetMapping
    public Page<ReturnRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listForCurrentCustomer(pageable);
    }

    @PostMapping("/{id}/package-info")
    public ReturnPackageFeeResponse setPackageInfo(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnPackageInfoRequest req
    ) {
        return returnService.setPackageInfoAndCalculateFee(id, req);
    }

    @PostMapping("/complaints")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createComplaint(@Valid @RequestBody CustomerReturnComplaintCreateRequest req) {
        complaintService.createComplaint(req);
    }
}
