package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerNotificationActionRequest;
import org.example.audio_ecommerce.dto.response.NotificationResponse;
import org.example.audio_ecommerce.service.CustomerNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/notifications")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final CustomerNotificationService customerNotificationService;

    @GetMapping
    public Page<NotificationResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return customerNotificationService.listForCurrentCustomer(keyword, read, pageable);
    }

    @GetMapping("/unread-count")
    public long countUnread() {
        return customerNotificationService.countUnreadForCurrentCustomer();
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID id) {
        customerNotificationService.markAsRead(id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        customerNotificationService.markAllAsReadForCurrentCustomer();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customerNotificationService.delete(id);
    }

    @PostMapping("/{id}/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void action(@PathVariable UUID id,
                       @RequestBody(required = false) CustomerNotificationActionRequest req) {
        customerNotificationService.performAction(id, req);
    }
}

