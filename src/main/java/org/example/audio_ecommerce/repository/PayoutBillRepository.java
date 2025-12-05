package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Enum.PayoutBillStatus;
import org.example.audio_ecommerce.entity.PayoutBill;
import org.example.audio_ecommerce.entity.PayoutBillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutBillRepository extends JpaRepository<PayoutBill, UUID> {


    PayoutBill findFirstByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, PayoutBillStatus status);
}
