package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.StoreNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.entity.Notification;
import org.example.audio_ecommerce.repository.NotificationRepository;
import org.example.audio_ecommerce.service.StoreNotificationService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreNotificationServiceImpl implements StoreNotificationService {

    private final NotificationRepository notificationRepo;
    private final SecurityUtils securityUtils;

    // ============= Helpers =============

    private UUID getCurrentStoreId() {
        return securityUtils.getCurrentStoreId();
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
        UUID storeId = getCurrentStoreId();
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));

        if (n.getTarget() != NotificationTarget.STORE || !storeId.equals(n.getTargetId())) {
            throw new IllegalArgumentException("Notification does not belong to current store");
        }
        return n;
    }

    // ============= Impl =============

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForCurrentStore(String keyword, Boolean read, Pageable pageable) {
        UUID storeId = getCurrentStoreId();

        // Simple version: không filter keyword/read (anh có thể custom thêm)
        Page<Notification> page = notificationRepo.findByTargetAndTargetId(
                NotificationTarget.STORE,
                storeId,
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
    public long countUnreadForCurrentStore() {
        UUID storeId = getCurrentStoreId();
        return notificationRepo.countByTargetAndTargetIdAndReadIsFalse(
                NotificationTarget.STORE,
                storeId
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
    public void markAllAsReadForCurrentStore() {
        UUID storeId = getCurrentStoreId();

        Pageable pageRequest = PageRequest.of(0, 200);
        Page<Notification> page;
        do {
            page = notificationRepo.findByTargetAndTargetId(
                    NotificationTarget.STORE,
                    storeId,
                    pageRequest
            );
            page.getContent().forEach(n -> {
                if (!n.isRead()) n.setRead(true);
            });
            notificationRepo.saveAll(page.getContent());
            if (!page.hasNext()) break;
            pageRequest = page.nextPageable();
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
    public void performAction(UUID notificationId, StoreNotificationActionRequest req) {
        Notification n = findOwnedNotificationOrThrow(notificationId);
        String action = (req != null && req.getAction() != null)
                ? req.getAction().toUpperCase()
                : "OPEN";

        log.info("Store {} performs action {} on notification {}", getCurrentStoreId(), action, notificationId);

        // Tuỳ logic anh muốn:
        // - Nếu type = NEW_ORDER: redirect tới actionUrl là /seller/orders/{id}
        // - Hoặc update status đơn,...
        // Ở backend anh có thể chỉ cần log + mark read, còn redirect để FE xử lý bằng actionUrl.
        n.setRead(true);
        notificationRepo.save(n);
    }
}
