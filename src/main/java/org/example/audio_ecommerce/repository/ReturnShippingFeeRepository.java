package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReturnShippingFeeRepository extends JpaRepository<ReturnShippingFee, UUID> {

    Optional<ReturnShippingFee> findByReturnRequestId(UUID returnRequestId);
}
