package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerReturnComplaint;
import org.example.audio_ecommerce.entity.Enum.ReturnComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CustomerReturnComplaintRepository extends JpaRepository<CustomerReturnComplaint, UUID> {

    List<CustomerReturnComplaint> findByStatusAndCreatedAtBefore(ReturnComplaintStatus status,
                                                                 LocalDateTime before);
}
