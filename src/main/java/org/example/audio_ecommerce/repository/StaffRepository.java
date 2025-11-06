package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Staff;
import org.example.audio_ecommerce.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    List<Staff> findByStore(Store store);
    Optional<Staff> findByIdAndStore(UUID id, Store store);
    boolean existsByAccount_Id(UUID accountId);
    Optional<Staff> findByAccount_Id(UUID accountId);
}