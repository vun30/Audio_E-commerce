package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.CustomerReturnComplaintCreateRequest;
import org.example.audio_ecommerce.entity.CustomerReturnComplaint;
import org.example.audio_ecommerce.entity.Enum.ReturnComplaintStatus;
import org.example.audio_ecommerce.entity.ReturnRequest;
import org.example.audio_ecommerce.repository.CustomerReturnComplaintRepository;
import org.example.audio_ecommerce.repository.ReturnRequestRepository;
import org.example.audio_ecommerce.service.CustomerReturnComplaintService;
import org.example.audio_ecommerce.service.WalletService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerReturnComplaintServiceImpl implements CustomerReturnComplaintService {

    private final CustomerReturnComplaintRepository complaintRepo;
    private final ReturnRequestRepository returnRepo;
    private final WalletService walletService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public void createComplaint(CustomerReturnComplaintCreateRequest req) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        ReturnRequest r = returnRepo.findById(req.getReturnRequestId())
                .orElseThrow(() -> new NoSuchElementException("ReturnRequest not found"));

        if (!r.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Not your return request");
        }

        CustomerReturnComplaint c = CustomerReturnComplaint.builder()
                .returnRequestId(r.getId())
                .customerId(r.getCustomerId())
                .shopId(r.getShopId())
                .orderItemId(r.getOrderItemId())
                .productId(r.getProductId())
                .productName(r.getProductName())
                .itemPrice(r.getItemPrice())
                .reason(req.getReason())
                .customerVideoUrl(req.getCustomerVideoUrl())
                .customerImageUrls(
                        Optional.ofNullable(req.getCustomerImageUrls())
                                .orElseGet(ArrayList::new)
                )
                .reasonType(r.getReasonType())
                .status(ReturnComplaintStatus.OPEN)
                .customerPhone(r.getCustomerPhone())
                .autoRefundExecuted(false)
                .build();

        complaintRepo.save(c);
    }

    @Override
    @Transactional
    public void processAutoRefundComplaints() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(2);
        List<CustomerReturnComplaint> list = complaintRepo
                .findByStatusAndCreatedAtBefore(ReturnComplaintStatus.OPEN, deadline);

        for (CustomerReturnComplaint c : list) {
            if (Boolean.TRUE.equals(c.getAutoRefundExecuted())) {
                continue;
            }

            ReturnRequest r = returnRepo.findById(c.getReturnRequestId())
                    .orElse(null);

            if (r == null) continue;

            walletService.forceRefundWithoutReturn(r);

            r.setStatus(org.example.audio_ecommerce.entity.Enum.ReturnStatus.AUTO_REFUNDED);
            r.setUpdatedAt(LocalDateTime.now());
            returnRepo.save(r);

            c.setStatus(ReturnComplaintStatus.RESOLVED);
            c.setAutoRefundExecuted(true);
            complaintRepo.save(c);
        }
    }
}
