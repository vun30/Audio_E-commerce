// service/Impl/DeviceTokenServiceImpl.java
package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.RegisterDeviceTokenRequest;
import org.example.audio_ecommerce.entity.DeviceToken;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.example.audio_ecommerce.repository.DeviceTokenRepository;
import org.example.audio_ecommerce.service.DeviceTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepo;

    @Override
    public void registerToken(NotificationTarget target,
                              UUID targetId,
                              RegisterDeviceTokenRequest req) {

        if (req.getToken() == null || req.getToken().isBlank())
            return;

        deviceTokenRepo.findByToken(req.getToken())
                .ifPresentOrElse(
                        token -> {
                            token.setTarget(target);
                            token.setTargetId(targetId);
                            token.setPlatform(req.getPlatform());
                            token.setLastUsedAt(LocalDateTime.now());
                            deviceTokenRepo.save(token);
                        },
                        () -> {
                            DeviceToken newToken = DeviceToken.builder()
                                    .target(target)
                                    .targetId(targetId)
                                    .token(req.getToken())
                                    .platform(req.getPlatform())
                                    .createdAt(LocalDateTime.now())
                                    .lastUsedAt(LocalDateTime.now())
                                    .build();
                            deviceTokenRepo.save(newToken);
                        }
                );
    }
}
