    package org.example.audio_ecommerce.repository;

    import org.example.audio_ecommerce.entity.PayoutReturnShippingFee;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.List;
    import java.util.UUID;

    public interface PayoutReturnShippingFeeRepository extends JpaRepository<PayoutReturnShippingFee, UUID> {

    List<PayoutReturnShippingFee> findByBill_Id(UUID billId);
}

