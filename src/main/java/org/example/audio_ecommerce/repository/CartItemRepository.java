//package org.example.audio_ecommerce.repository;
//
//import org.example.audio_ecommerce.entity.CartItem;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//import java.util.UUID;
//
//public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
//    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(UUID cartId, UUID productId);
//    Optional<CartItem> findByCart_CartIdAndCombo_ComboId(UUID cartId, UUID comboId);
//}
