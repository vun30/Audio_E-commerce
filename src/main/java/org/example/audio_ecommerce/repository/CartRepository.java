package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Cart;
import org.example.audio_ecommerce.entity.Enum.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByOwnerIdAndStatus(UUID ownerId, CartStatus status);
}
