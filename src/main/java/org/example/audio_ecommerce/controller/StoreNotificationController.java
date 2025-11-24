package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.example.audio_ecommerce.service.StoreNotificationService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/store/notifications")
@RequiredArgsConstructor
public class StoreNotificationController {

    private final StoreNotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationService.listForCurrentStore(keyword, read, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        long count = notificationService.countUnreadForCurrentStore();
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllAsReadForCurrentStore();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        notificationService.delete(id);
    }

    @PostMapping("/{id}/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void action(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) StoreNotificationActionRequest req
    ) {
        notificationService.performAction(id, req);
    }
}
