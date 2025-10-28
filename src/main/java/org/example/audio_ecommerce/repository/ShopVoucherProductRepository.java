package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.ShopVoucherProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopVoucherProductRepository extends JpaRepository<ShopVoucherProduct, UUID> {

       boolean existsByProduct_ProductIdAndVoucher_Status(UUID productId, VoucherStatus status);

       // 🔍 Lấy voucher ACTIVE áp dụng cho 1 sản phẩm
    Optional<ShopVoucherProduct> findFirstByProduct_ProductIdAndVoucher_Status(UUID productId, VoucherStatus status);
}
