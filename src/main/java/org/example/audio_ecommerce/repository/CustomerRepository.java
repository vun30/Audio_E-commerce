package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // ==== Bổ sung cho luồng tạo Customer khi đăng ký Account ====
    boolean existsByAccount_Id(UUID accountId);
    Optional<Customer> findByAccount_Id(UUID accountId);

    // ==== Phần bạn đã có ====
    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    @Query("""
        select c from Customer c
        where (:status is null or c.status = :status)
          and ( :kw is null
                or lower(c.fullName) like lower(concat('%', :kw, '%'))
                or lower(c.email)    like lower(concat('%', :kw, '%'))
                or c.phoneNumber     like concat('%', :kw, '%')
              )
        """)
    Page<Customer> search(@Param("kw") String keyword,
                          @Param("status") CustomerStatus status,
                          Pageable pageable);
}
