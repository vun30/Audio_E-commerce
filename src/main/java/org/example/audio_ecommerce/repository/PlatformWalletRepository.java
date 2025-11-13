package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PlatformWallet;
import org.example.audio_ecommerce.entity.Enum.WalletOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, UUID> {

    List<PlatformWallet> findByOwnerType(WalletOwnerType ownerType);

    Optional<PlatformWallet> findByOwnerTypeAndOwnerId(WalletOwnerType ownerType, UUID ownerId);

    Optional<PlatformWallet> findFirstByOwnerType(WalletOwnerType ownerType);


}
