package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.CustomerNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.entity.Notification;
import org.example.audio_ecommerce.repository.NotificationRepository;
import org.example.audio_ecommerce.service.CustomerNotificationService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationServiceImpl implements CustomerNotificationService {

    private final NotificationRepository notificationRepo;
    private final SecurityUtils securityUtils;

    // ================= Helpers =================

    private UUID getCurrentCustomerId() {
        return securityUtils.getCurrentCustomerId();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .actionUrl(n.getActionUrl())
                .metadataJson(n.getMetadataJson())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private Notification findOwnedNotificationOrThrow(UUID id) {
        UUID customerId = getCurrentCustomerId();
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));

        if (n.getTarget() != NotificationTarget.CUSTOMER || !customerId.equals(n.getTargetId())) {
            throw new IllegalArgumentException("Notification does not belong to current customer");
        }
        return n;
    }

    // ================= Impl =================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForCurrentCustomer(String keyword, Boolean read, Pageable pageable) {
        UUID customerId = getCurrentCustomerId();

        // phiên bản đơn giản: chưa filter keyword/read
        Page<Notification> page = notificationRepo.findByTargetAndTargetId(
                NotificationTarget.CUSTOMER,
                customerId,
                pageable
        );

        return new PageImpl<>(
                page.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadForCurrentCustomer() {
        UUID customerId = getCurrentCustomerId();
        return notificationRepo.countByTargetAndTargetIdAndReadIsFalse(
                NotificationTarget.CUSTOMER,
                customerId
        );
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification n = findOwnedNotificationOrThrow(notificationId);
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepo.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllAsReadForCurrentCustomer() {
        UUID customerId = getCurrentCustomerId();

        int page = 0;
        int size = 200;
        Page<Notification> notifPage;

        do {
            notifPage = notificationRepo.findByTargetAndTargetId(
                    NotificationTarget.CUSTOMER,
                    customerId,
                    PageRequest.of(page, size)
            );

            notifPage.getContent().forEach(n -> {
                if (!n.isRead()) n.setRead(true);
            });
            notificationRepo.saveAll(notifPage.getContent());

            if (!notifPage.hasNext()) break;
            page++;
        } while (true);
    }

    @Override
    @Transactional
    public void delete(UUID notificationId) {
        Notification n = findOwnedNotificationOrThrow(notificationId);
        notificationRepo.delete(n);
    }

    @Override
    @Transactional
    public void performAction(UUID notificationId, CustomerNotificationActionRequest req) {
        Notification n = findOwnedNotificationOrThrow(notificationId);
        String action = (req != null && req.getAction() != null)
                ? req.getAction().toUpperCase()
                : "OPEN";

        log.info("Customer {} performs action {} on notification {}",
                getCurrentCustomerId(), action, notificationId);

        // Backend chỉ cần mark read + log.
        // FE dựa vào actionUrl + type để redirect (ví dụ: /customer/orders/{id})
        n.setRead(true);
        notificationRepo.save(n);
    }
}
