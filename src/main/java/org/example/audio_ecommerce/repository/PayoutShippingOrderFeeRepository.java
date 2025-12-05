package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.PayoutShippingOrderFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutShippingOrderFeeRepository extends JpaRepository<PayoutShippingOrderFee, UUID> {

    List<PayoutShippingOrderFee> findByBill_Id(UUID billId);
}