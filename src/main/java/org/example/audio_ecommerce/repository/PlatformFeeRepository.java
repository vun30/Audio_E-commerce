package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PlatformFeeRepository extends JpaRepository<PlatformFee, UUID> {
    Optional<PlatformFee> findByIsActiveTrue();

    Optional<PlatformFee> findFirstByIsActiveTrueOrderByEffectiveDateDesc();
}
