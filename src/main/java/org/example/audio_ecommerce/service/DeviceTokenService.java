package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.RegisterDeviceTokenRequest;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;

import java.util.UUID;

public interface DeviceTokenService {

    void registerToken(NotificationTarget target,
                       UUID targetId,
                       RegisterDeviceTokenRequest req);
}
