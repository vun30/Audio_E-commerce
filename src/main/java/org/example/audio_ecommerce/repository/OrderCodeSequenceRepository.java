package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.OrderCodeSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface OrderCodeSequenceRepository extends JpaRepository<OrderCodeSequence, Long> {
    Optional<OrderCodeSequence> findByOrderDate(LocalDate orderDate);
}
