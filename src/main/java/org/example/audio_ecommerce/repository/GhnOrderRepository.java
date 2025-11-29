package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.entity.GhnOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GhnOrderRepository extends JpaRepository<GhnOrder, UUID> {
    Optional<GhnOrder> findByStoreOrderId(UUID storeOrderId);
    boolean existsByStoreOrderId(UUID storeOrderId);
    // lấy các order đang “active” để sync
    List<GhnOrder> findAllByStatusIn(Collection<GhnStatus> statuses);
}
