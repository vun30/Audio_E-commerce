package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.ShopVoucher;
import org.example.audio_ecommerce.entity.ShopVoucherProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ShopVoucherProductRepository extends JpaRepository<ShopVoucherProduct, UUID> {

       boolean existsByProduct_ProductIdAndVoucher_Status(UUID productId, VoucherStatus status);

       // 🔍 Lấy voucher ACTIVE áp dụng cho 1 sản phẩm
    Optional<ShopVoucherProduct> findFirstByProduct_ProductIdAndVoucher_Status(UUID productId, VoucherStatus status);


    @Query("""
        SELECT svp.voucher
        FROM ShopVoucherProduct svp
        WHERE svp.product.productId = :productId
          AND svp.voucher.status = 'ACTIVE'
          AND :now BETWEEN svp.voucher.startTime AND svp.voucher.endTime
    """)
    Optional<ShopVoucher> findActiveVoucherByProduct(
            @Param("productId") UUID productId,
            @Param("now") LocalDateTime now);
}
