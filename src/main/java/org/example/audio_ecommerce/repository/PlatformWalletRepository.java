package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, UUID> {

    List<PlatformWallet> findByOwnerType(WalletOwnerType ownerType);

    Optional<PlatformWallet> findByOwnerTypeAndOwnerId(WalletOwnerType ownerType, UUID ownerId);

    Optional<PlatformWallet> findFirstByOwnerType(WalletOwnerType ownerType);

    @Query("""
        select w from PlatformWallet w
        where w.ownerType = org.example.audio_ecommerce.entity.Enum.WalletOwnerType.PLATFORM
          and w.ownerId is null
    """)
    Optional<PlatformWallet> findMainPlatformWallet();

    //repo cho rút tiền
    Optional<PlatformWallet> findByOwnerTypeAndOwnerIdIsNull(WalletOwnerType ownerType);

    /**
     * Helper dùng cho cashBalance (luôn phải tồn tại 1 record)
     */
    default PlatformWallet getPlatformMainWallet() {
        return findByOwnerTypeAndOwnerIdIsNull(WalletOwnerType.PLATFORM)
                .orElseThrow(() ->
                        new IllegalStateException("PLATFORM wallet not found")
                );
    }


//    @Query(value = """
//        SELECT
//            so.status           AS status,
//            so.created_at       AS createdAt,
//            so.delivered_at     AS deliveredAt,
//            so.shipping_fee_real AS shippingFeeReal,
//            so.shipping_fee     AS shippingFee,
//            so.paid_by_shop     AS paidByShop
//        FROM store_order so
//        WHERE (:from IS NULL OR so.created_at >= :from)
//          AND (:to   IS NULL OR so.created_at <= :to)
//          AND so.status NOT IN ('UNPAID','CONFIRMED','AWAITING_SHIPMENT','CANCELLED','PENDING')
//    """, nativeQuery = true)
//    List<FlatDebtOrderRow> findFlatDebtRows(@Param("from") LocalDateTime from,
//                                            @Param("to") LocalDateTime to);

}
