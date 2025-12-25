package org.example.audio_ecommerce.repository;

import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.Enum.CustomerStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
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

     Optional<Customer> findByAccount_Email(String email);
    Optional<Customer> findByAccount(Account account);
    @Query(value = """
        select count(*)
        from customers c
        inner join accounts a on c.account_id = a.id
        where c.created_at >= :from and c.created_at < :to
          and a.role = 'CUSTOMER'
    """, nativeQuery = true)
    long countNewCustomersInRange(@Param("from") String from, @Param("to") String to);

    @Query(value = """
        select month(c.created_at) as m, count(*) as cnt
        from customers c
        inner join accounts a on c.account_id = a.id
        where year(c.created_at) = :year
          and a.role = 'CUSTOMER'
        group by month(c.created_at)
    """, nativeQuery = true)
    java.util.List<Object[]> countNewCustomersByMonth(@Param("year") int year);

    List<Customer> findAllByBuyableFalseAndLegalPointZeroedAtIsNotNullAndLegalPoint(BigDecimal legalPoint);

}
