package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.SellBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SellBannerRepository extends JpaRepository<SellBanner, UUID> {
    List<SellBanner> findAllByActiveTrueOrderByCreatedAtDesc();
}
