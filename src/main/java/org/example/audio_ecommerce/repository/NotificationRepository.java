package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Notification;
import org.example.audio_ecommerce.entity.Enum.NotificationTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTargetAndTargetId(
            NotificationTarget target,
            UUID targetId,
            Pageable pageable
    );

    long countByTargetAndTargetIdAndReadIsFalse(
            NotificationTarget target,
            UUID targetId
    );
}
