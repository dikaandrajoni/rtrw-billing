package id.rtrwnet.billing.repository;

import id.rtrwnet.billing.domain.Invoice;
import id.rtrwnet.billing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByCustomerIdAndPeriod(Long customerId, String period);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Invoice> findWithCustomerById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    List<Invoice> findByPeriodOrderByIdAsc(String period);

    @EntityGraph(attributePaths = {"customer"})
    List<Invoice> findByCustomerIdOrderByPeriodDesc(Long customerId);

    /** Tagihan belum dibayar yang sudah lewat batas (jatuh tempo + masa tenggang). */
    @EntityGraph(attributePaths = {"customer", "customer.plan", "customer.router"})
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    /** Tagihan belum dibayar yang jatuh tempo di rentang tanggal tertentu, untuk pengingat. */
    @EntityGraph(attributePaths = {"customer"})
    List<Invoice> findByStatusAndReminderSentFalseAndDueDateBetween(InvoiceStatus status, LocalDate from, LocalDate to);

    boolean existsByCustomerIdAndStatusAndDueDateBefore(Long customerId, InvoiceStatus status, LocalDate date);

    @Query("select coalesce(sum(i.amount), 0) from Invoice i where i.period = :period and i.status = :status")
    BigDecimal sumAmount(@Param("period") String period, @Param("status") InvoiceStatus status);

    long countByPeriodAndStatus(String period, InvoiceStatus status);
}
