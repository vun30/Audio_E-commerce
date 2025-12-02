// controller/CustomerDeviceController.java
package org.example.audio_ecommerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.RegisterDeviceTokenRequest;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.service.DeviceTokenService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers/me/devices")
@RequiredArgsConstructor
public class CustomerDeviceController {

    private final DeviceTokenService deviceTokenService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody RegisterDeviceTokenRequest req) {
        UUID customerId = securityUtils.getCurrentCustomerId();
        deviceTokenService.registerToken(NotificationTarget.CUSTOMER, customerId, req);
    }
}
