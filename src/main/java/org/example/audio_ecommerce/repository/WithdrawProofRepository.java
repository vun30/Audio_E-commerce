package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.WithdrawProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WithdrawProofRepository extends JpaRepository<WithdrawProof, UUID> {

    long countByWithdrawRequestId(UUID withdrawRequestId);
    List<WithdrawProof> findByWithdrawRequestIdOrderByCreatedAtDesc(UUID withdrawRequestId);
    List<WithdrawProof> findByWithdrawRequestId(UUID withdrawRequestId); // for admin
}

