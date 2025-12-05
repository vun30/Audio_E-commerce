package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PayoutBillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutBillItemRepository extends JpaRepository<PayoutBillItem, UUID> {

    List<PayoutBillItem> findByBill_Id(UUID billId);
}
