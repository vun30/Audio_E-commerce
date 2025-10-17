package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ShopVoucherProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShopVoucherProductRepository extends JpaRepository<ShopVoucherProduct, UUID> {
}
