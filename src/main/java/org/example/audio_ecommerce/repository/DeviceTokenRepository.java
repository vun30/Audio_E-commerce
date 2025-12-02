package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.DeviceToken;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByTargetAndTargetId(NotificationTarget target, UUID targetId);

    Optional<DeviceToken> findByToken(String token);
}
