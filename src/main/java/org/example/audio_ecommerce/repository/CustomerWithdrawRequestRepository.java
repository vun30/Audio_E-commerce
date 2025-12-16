package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerWithdrawRequestRepository extends JpaRepository<CustomerWithdrawRequest, UUID> {

    Page<CustomerWithdrawRequest> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<CustomerWithdrawRequest> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            UUID customerId, WithdrawRequestStatus status, Pageable pageable
    );

    Page<CustomerWithdrawRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<CustomerWithdrawRequest> findByStatusOrderByCreatedAtDesc(WithdrawRequestStatus status, Pageable pageable);
    boolean existsByIdAndCustomerId(UUID id, UUID customerId);
}
