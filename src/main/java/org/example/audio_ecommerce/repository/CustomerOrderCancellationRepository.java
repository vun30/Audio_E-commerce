// repository/CustomerOrderCancellationRepository.java
package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.CustomerOrderCancellationRequest;
import org.example.audio_ecommerce.entity.Enum.CancellationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerOrderCancellationRepository
        extends JpaRepository<CustomerOrderCancellationRequest, UUID> {

    List<CustomerOrderCancellationRequest> findAllByCustomerOrder_Id(UUID customerOrderId);

    List<CustomerOrderCancellationRequest> findAllByCustomerOrder_Customer_Id(UUID customerId);

    boolean existsByStoreOrder_IdAndStatus(UUID storeOrderId, CancellationRequestStatus status);
    boolean existsByCustomerOrder_IdAndStatus(UUID customerOrderId, CancellationRequestStatus status);

}
