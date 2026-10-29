package id.rtrwnet.billing.repository;

import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByPppoeUsername(String pppoeUsername);

    long countByStatus(CustomerStatus status);

    @EntityGraph(attributePaths = {"plan", "router"})
    Optional<Customer> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"plan", "router"})
    List<Customer> findByStatusIn(List<CustomerStatus> statuses);

    @EntityGraph(attributePaths = {"plan", "router"})
    @Query("""
            select c from Customer c
            where (:q = ''
                   or lower(c.name) like concat('%', :q, '%')
                   or lower(c.code) like concat('%', :q, '%')
                   or lower(c.pppoeUsername) like concat('%', :q, '%'))
              and (:status is null or c.status = :status)
            """)
    Page<Customer> search(@Param("q") String q, @Param("status") CustomerStatus status, Pageable pageable);
}
