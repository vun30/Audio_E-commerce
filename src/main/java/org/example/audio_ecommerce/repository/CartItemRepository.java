package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

}
